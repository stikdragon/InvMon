package uk.co.stikman.invmon.htmlout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.inverter.util.InvUtil;

public class PageLayout {

	private boolean											def		= false;
	private String											id;
	private List<PageWidget>								widgets	= new ArrayList<>();

	private static final Map<String, Supplier<PageWidget>>	TYPES	= new HashMap<>();

	static {
		TYPES.put("chart", ChartWidget::new);
		TYPES.put("table-pv", PVTableWidget::new);
		TYPES.put("infobit", InfoBitWidget::new);
		TYPES.put("timesel", TimeSelPageWidget::new);
		TYPES.put("controls", ControlsWidget::new);
	}

	public void configure(Element root) {
		this.id = InvUtil.getAttrib(root, "id");
		this.def = Boolean.parseBoolean(InvUtil.getAttrib(root, "default", "false"));
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

}
