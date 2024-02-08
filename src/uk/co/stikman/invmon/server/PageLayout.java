package uk.co.stikman.invmon.server;

import static uk.co.stikman.invmon.inverter.util.InvUtil.loadXML;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.InvMonException;
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

	private final Env													env;
	private final File													file;
	private boolean														def					= false;
	private String														id;
	private List<PageWidget>											widgets				= new ArrayList<>();
	private long														lastModifiedTime	= 0;

	private static final Map<String, Function<PageLayout, PageWidget>>	TYPES				= new HashMap<>();

	static {
		TYPES.put("chart", ChartWidget::new);
		TYPES.put("table-pv", PVTableWidget::new);
		TYPES.put("infobit", InfoBitWidget::new);
		TYPES.put("timesel", TimeSelectorWidget::new);
		TYPES.put("controls", ControlsWidget::new);
		TYPES.put("energysummary", DailyPowerSummaryWidget::new);
		TYPES.put("dial", GaugeWidget::new);
		TYPES.put("text-summary", TextSummaryWidget::new);
		TYPES.put("stik-controller", StikSystemControllerWidget::new);
		TYPES.put("log", LogWidget::new);
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
	
	public void fromDOM(MDElement root) {
		widgets.clear();
		for (MDElement el : root.getElements("Widget")) {
			String cls = el.getAttrib("class");
			Function<PageLayout, PageWidget> ctor = TYPES.get(cls);
			if (ctor == null)
				throw new NoSuchElementException("Widget class [" + cls + "] not found");
			PageWidget w = ctor.apply(this);
			w.configure(el);
			widgets.add(w);
		}
	}

	public void toDOM(MiniDOM root) {
		for (PageWidget w : widgets) {
			MDElement el = new MDElement("Widget");
			w.toDOM(el);
			root.append(el);
		}
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
