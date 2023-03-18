package uk.co.stikman.invmon.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;

import org.json.JSONArray;
import org.json.JSONObject;
import org.teavm.jso.browser.History;
import org.teavm.jso.browser.Location;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

public class MainPage extends ClientPage {
	private HTMLElement														root;

	private List<AbstractPageWidget>										widgets		= new ArrayList<>();
	private int																gridSize;
	private static Map<String, Function<ClientPage, AbstractPageWidget>>	pageTypes	= new HashMap<>();

	static {
		pageTypes.put("timesel", TimeSelector::new);
		pageTypes.put("infobit", InfoBitWidget::new);
		pageTypes.put("chart", ChartWidget::new);
		pageTypes.put("controls", ControlsWidget::new);
	}

	public MainPage() {
		super();
		root = InvMon.div("mainpage");
	}

	@Override
	public HTMLElement getElement() {
		return root;
	}

	public void load() {
		//
		// query server for layout
		//
		fetch("getConfig", new JSONObject(), result -> {
			gridSize = result.getInt("gridSize");
			JSONArray arr = result.getJSONArray("widgets");
			for (int i = 0; i < arr.length(); ++i) {
				JSONObject obj = arr.getJSONObject(i);
				Function<ClientPage, AbstractPageWidget> s = pageTypes.get(obj.getString("type"));
				if (s == null)
					throw new NoSuchElementException("Unknown widget: " + obj.getString("type"));
				AbstractPageWidget w = s.apply(this);
				w.configure(obj);
				w.construct(root);
				widgets.add(w);

				if (w instanceof TimeSelector) {
					String param = ClientUtil.getURLParam("dur", null);
					if (param != null)
						((TimeSelector) w).setCurrent(Integer.parseInt(param));
					((TimeSelector) w).setOnChange(this::setDataRange);
				}
			}

			refresh();
		});
	}

	public void refresh() {
		widgets.forEach(w -> w.refresh());
	}

	public int getGridSize() {
		return gridSize;
	}

	public void setDataRange(int offset, int duration) {
		Location.current().setHash("dur=" + duration);
		JSONObject jo = new JSONObject();
		jo.put("off", offset);
		jo.put("dur", duration);
		post("setParams", jo, res -> refresh());
	}

}
