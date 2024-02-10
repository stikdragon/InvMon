package uk.co.stikman.invmon.server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.Pair;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.minidom.MDElement;
import uk.co.stikman.invmon.minidom.MiniDOM;
import uk.co.stikman.invmon.server.widgets.ChartWidget;
import uk.co.stikman.invmon.server.widgets.ControlsWidget;
import uk.co.stikman.invmon.server.widgets.DailyPowerSummaryWidget;
import uk.co.stikman.invmon.server.widgets.GaugeWidget;
import uk.co.stikman.invmon.server.widgets.InfoBitWidget;
import uk.co.stikman.invmon.server.widgets.LogWidget;
import uk.co.stikman.invmon.server.widgets.PVTableWidget;
import uk.co.stikman.invmon.server.widgets.PageWidget;
import uk.co.stikman.invmon.server.widgets.StikSystemControllerWidget;
import uk.co.stikman.invmon.server.widgets.TextSummaryWidget;
import uk.co.stikman.invmon.server.widgets.TimeSelectorWidget;

public class PageLayout {

	private final Env								env;
	private final File								file;
	private boolean									def					= false;
	private String									id;
	private List<PageWidget>						widgets				= new ArrayList<>();
	private long									lastModifiedTime	= 0;

	private static final List<Pair<String, String>>	TYPES				= new ArrayList<>();

	static {
		TYPES.add(new Pair<>("chart", "ChartWidget"));
		TYPES.add(new Pair<>("table-pv", "PVTableWidget"));
		TYPES.add(new Pair<>("infobit", "InfoBitWidget"));
		TYPES.add(new Pair<>("timesel", "TimeSelectorWidget"));
		TYPES.add(new Pair<>("controls", "ControlsWidget"));
		TYPES.add(new Pair<>("energysummary", "DailyPowerSummaryWidget"));
		TYPES.add(new Pair<>("dial", "GaugeWidget"));
		TYPES.add(new Pair<>("text-summary", "TextSummaryWidget"));
		TYPES.add(new Pair<>("stik-controller", "StikSystemControllerWidget"));
		TYPES.add(new Pair<>("log", "LogWidget"));
	}

	public PageLayout(Env env, File file) {
		this.env = env;
		this.file = file;
	}

	public void configure(MDElement root) {
		this.id = root.getAttrib("id");
		this.def = Boolean.parseBoolean(root.getAttrib("default", "false"));
	}

	public void loadFromSource() throws InvMonException {
		try {
			MiniDOM doc = InvUtil.loadMiniDOM(file);
			fromDOM(doc);
			lastModifiedTime = file.lastModified();
		} catch (IOException e) {
			throw new InvMonException("Failed to load config for page: " + getId() + " because: " + e.getMessage(), e);
		}
	}

	public void saveToSource() throws InvMonException {
		try {
			MiniDOM doc = new MiniDOM();
			toDOM(doc);
			InvUtil.writeMiniDOM(doc, file);
			lastModifiedTime = file.lastModified();
		} catch (IOException e) {
			throw new InvMonException("Failed to save config for page: " + getId() + " because: " + e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public void fromDOM(MDElement root) throws InvMonException {
		widgets.clear();
		for (MDElement el : root.getElements("Widget")) {
			String cls = el.getAttrib("class");

			Pair<String, String> p = null;
			for (Pair<String, String> x : TYPES) {
				if (x.getA().equals(cls))
					p = x;
			}

			if (p == null)
				throw new NoSuchElementException("Widget class [" + cls + "] not found");
			PageWidget w;
			try {
				Class<?> clazz = Class.forName("uk.co.stikman.invmon.server.widgets." + p.getB());
				Constructor<PageWidget> ctor = (Constructor<PageWidget>) clazz.getConstructor(PageLayout.class);
				w = ctor.newInstance(this);
			} catch (Exception e) {
				throw new InvMonException("Could not construct page widget of class [" + cls + "] because: " + e.getMessage(), e);
			}
			w.fromDOM(el);
			widgets.add(w);
		}
	}

	public void toDOM(MiniDOM root) {
		for (PageWidget w : widgets) {
			MDElement el = new MDElement("Widget");
			el.setAttrib("class", getConfigClassNameFor(w.getClass()));
			w.toDOM(el);
			root.append(el);
		}
	}

	private String getConfigClassNameFor(Class<? extends PageWidget> typ) {
		for (Pair<String, String> p : TYPES) {
			if (p.getB().equals(typ.getSimpleName()))
				return p.getA();
		}
		throw new NoSuchElementException("Unknown PageWidget type: " + typ.toString());
	}

	public boolean isDefault() {
		return def;
	}

	public String getId() {
		return id;
	}

	public List<PageWidget> getWidgets() {
		return widgets;
	}

	@Override
	public String toString() {
		return id;
	}

	@SuppressWarnings("unchecked")
	public <T extends PageWidget> T getWidgetById(String name) {
		for (PageWidget wij : getWidgets())
			if (wij.getId().equals(name))
				return (T) wij;
		throw new NoSuchElementException("Widget [" + name + "] not found");
	}

	public File getFile() {
		return file;
	}

	public long getLastModifiedTime() {
		return lastModifiedTime;
	}

	public Env getEnv() {
		return env;
	}

}
