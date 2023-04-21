package uk.co.stikman.invmon.server;

import static uk.co.stikman.invmon.inverter.util.InvUtil.getAttrib;
import static uk.co.stikman.invmon.inverter.util.InvUtil.getElement;
import static uk.co.stikman.invmon.inverter.util.InvUtil.getElements;
import static uk.co.stikman.invmon.inverter.util.InvUtil.loadXML;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvModDefinition;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.server.widgets.ChartWidget;
import uk.co.stikman.invmon.server.widgets.ControlsWidget;
import uk.co.stikman.invmon.server.widgets.DailyPowerSummaryWidget;
import uk.co.stikman.invmon.server.widgets.GaugeWidget;
import uk.co.stikman.invmon.server.widgets.InfoBitWidget;
import uk.co.stikman.invmon.server.widgets.PVTableWidget;
import uk.co.stikman.invmon.server.widgets.PageWidget;
import uk.co.stikman.invmon.server.widgets.TimeSelPageWidget;

public class PageLayout {

	private boolean											def					= false;
	private String											id;
	private List<PageWidget>								widgets				= new ArrayList<>();
	private File											file;
	private long											lastModifiedTime	= 0;

	private static final Map<String, Supplier<PageWidget>>	TYPES				= new HashMap<>();

	static {
		TYPES.put("chart", ChartWidget::new);
		TYPES.put("table-pv", PVTableWidget::new);
		TYPES.put("infobit", InfoBitWidget::new);
		TYPES.put("timesel", TimeSelPageWidget::new);
		TYPES.put("controls", ControlsWidget::new);
		TYPES.put("energysummary", DailyPowerSummaryWidget::new);
		TYPES.put("dial", GaugeWidget::new);
	}

	public PageLayout(File file) {
		this.file = file;
	}

	public void configure(Element root) {
		this.id = InvUtil.getAttrib(root, "id");
		this.def = Boolean.parseBoolean(InvUtil.getAttrib(root, "default", "false"));
	}

	public void loadFromSource() throws InvMonException {
		try {
			Document doc = loadXML(file);
			fromDOM(doc.getDocumentElement());
			lastModifiedTime = file.lastModified();
		} catch (IOException e) {
			throw new InvMonException("Failed to load config for page: " + getId() + " because: " + e.getMessage(), e);
		}
	}

	public void fromDOM(Element root) {
		widgets.clear();
		for (Element el : InvUtil.getElements(root, "Widget")) {
			String cls = InvUtil.getAttrib(el, "class");
			Supplier<PageWidget> ctor = TYPES.get(cls);
			if (ctor == null)
				throw new NoSuchElementException("Widget class [" + cls + "] not found");
			PageWidget w = ctor.get();
			w.configure(el);
			widgets.add(w);
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

	public PageWidget getWidgetById(String name) {
		for (PageWidget wij : getWidgets())
			if (wij.getId().equals(name))
				return wij;
		throw new NoSuchElementException("Widget [" + name + "] not found");
	}

	public File getFile() {
		return file;
	}

	public long getLastModifiedTime() {
		return lastModifiedTime;
	}

}
