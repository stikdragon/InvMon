package uk.co.stikman.invmon.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONObject;
import org.teavm.jso.browser.Location;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.client.wij.BMSStatusWidget;
import uk.co.stikman.invmon.client.MessagePopup.Type;
import uk.co.stikman.invmon.client.wij.ChartWidget;
import uk.co.stikman.invmon.client.wij.ControlsWidget;
import uk.co.stikman.invmon.client.wij.InfoBitWidget;
import uk.co.stikman.invmon.client.wij.InverterControlWidgetStik;
import uk.co.stikman.invmon.client.wij.LogWidget;
import uk.co.stikman.invmon.client.wij.ServerRenderedWidget;
import uk.co.stikman.invmon.client.wij.TextSummaryWidget;
import uk.co.stikman.invmon.client.wij.TimeSelectorWidget;
import uk.co.stikman.invmon.inverter.util.InvUtil;

/**
 * a standard page that contains widgets
 * 
 * @author stik
 *
 */
public class StandardPage extends ClientPage {
	private HTMLElement														root;

	private List<AbstractPageWidget>										widgets				= new ArrayList<>();
	private int																gridSize;

	private Boolean															doAutoRefresh		= false;

	private boolean															lastRequestFinished	= true;

	private ConsolePopup													console;

	private HTMLElement														configButton;

	private String															pageName;
	private static Map<String, Function<StandardPage, AbstractPageWidget>>	pageTypes			= new HashMap<>();

	static {
		pageTypes.put("timesel", TimeSelectorWidget::new);
		pageTypes.put("infobit", InfoBitWidget::new);
		pageTypes.put("chart", ChartWidget::new);
		pageTypes.put("serverside", ServerRenderedWidget::new);
		pageTypes.put("controls", ControlsWidget::new);
		pageTypes.put("text-summary", TextSummaryWidget::new);
		pageTypes.put("log", LogWidget::new);
		pageTypes.put("stik-controller", InverterControlWidgetStik::new);
		pageTypes.put("bms-status", BMSStatusWidget::new);
	}

	public StandardPage() {
		super();
		root = InvMon.div("mainpage");
		getBus().subscribe(Events.AUTOREFRESH_CHANGED, data -> doAutoRefresh = (Boolean) data);
		getBus().subscribe(Events.REFRESH_NOW, data -> refresh(false));

		Window.setInterval(() -> {
			if (lastRequestFinished)
				if (doAutoRefresh)
					refresh(true);
		}, 5000);

		fetch("getUserDetails", new JSONObject(), resp -> {
			if (resp.has("name")) {
				LoggedInUser u = new LoggedInUser();
				u.setName(resp.getString("name"));
				u.setToken(resp.getString("token"));
				InvMon.INSTANCE.setUser(u);
				getBus().fire(Events.USER_LOGGED_IN, u);
			}
		});

		//
		// bind a global keypress for the console
		//
		root.getOwnerDocument().addEventListener("keydown", ev -> {
			KeyboardEvent ev2 = ev.cast();
			if (ev2.isCtrlKey() && ev2.isShiftKey() && ev2.getCode().equals("KeyA")) {
				showConsole();
				ev2.preventDefault();
			}
		});

		configButton = InvMon.div("config-button");
		HTMLElement span = InvMon.element("span", "icon");
		configButton.appendChild(span);
		root.appendChild(configButton);
		configButton.addEventListener("click", ev -> {
			ev.stopPropagation();
			MouseEvent ev2 = (MouseEvent) ev;
			Menu mnu = new Menu();
			mnu.addItem("Login", () -> doLogin());
			mnu.addItem("Show log..", this::showLogWindow);
			mnu.addItem("Show layout", this::showLayout);
			mnu.addItem("Console..", this::showConsole);
			mnu.addItem("Save page config", this::savePageConfig);
			mnu.showAt(ev2.getClientX(), ev2.getClientY());
		});
	}

	private void savePageConfig() {
		JSONObject jo = new JSONObject();
		jo.put("page", pageName);
		post("saveConfig", jo, res -> {
			InvMon.getMessagePopup().addMessage("Saved", Type.MESSAGE);
		});
	}

	private void showLogWindow() {
		fetch("fetchUserLog", new JSONObject(), result -> {
			TextPopup wnd = new TextPopup(this, result.getString("log"));
			wnd.showModal();
		}, err -> {
			ClientUtil.handleError(err);
		});
	}

	private void showLayout() {
		int gs = getGridSize();
		StringBuilder sb = new StringBuilder();
		for (AbstractPageWidget w : getWidgets()) {
			sb.append(w.getId()).append(": ").append(w.getName()).append(" - ");
			sb.append((int) (w.getX() / gs)).append(", ");
			sb.append((int) (w.getY() / gs)).append(", ");
			sb.append((int) (w.getWidth() / gs)).append(", ");
			sb.append((int) (w.getHeight() / gs)).append("\n");
		}
		TextPopup wnd = new TextPopup(this, sb.toString());
		wnd.showModal();
	}

	@Override
	public HTMLElement getElement() {
		return root;
	}

	public void load() {
		Map<String, String> urlparams = InvUtil.getLocationParameters(Window.current().getLocation().getSearch());
		Map<String, String> hashparams = InvUtil.getLocationParameters(Window.current().getLocation().getHash());
		setDataRange(hashparams.containsKey("off") ? Integer.parseInt(hashparams.get("off")) : 0, hashparams.containsKey("dur") ? Integer.parseInt(hashparams.get("dur")) : 60);
		this.pageName = urlparams.get("layout");

		//
		// query server for layout
		//
		JSONObject jo = new JSONObject();
		jo.put("page", pageName);
		fetch("getConfig", jo, result -> {
			gridSize = result.getInt("gridSize");
			JSONArray arr = result.getJSONArray("widgets");
			for (int i = 0; i < arr.length(); ++i) {
				JSONObject obj = arr.getJSONObject(i);
				Function<StandardPage, AbstractPageWidget> s = pageTypes.get(obj.getString("type"));
				if (s == null)
					throw new NoSuchElementException("Unknown widget: " + obj.getString("type"));
				AbstractPageWidget w = s.apply(this);
				w.configure(obj);
				w.construct(root);
				widgets.add(w);

				if (w instanceof TimeSelectorWidget) {
					String param = ClientUtil.getURLParam("dur", null);
					if (param != null)
						((TimeSelectorWidget) w).setCurrent(Integer.parseInt(param));
					((TimeSelectorWidget) w).setOnChange(this::setDataRange);
				}
			}

			refresh(false);
		});
	}

	public void refresh(boolean nomask) {
		lastRequestFinished = false;
		post("invalidateResults", new JSONObject(), res -> {
			widgets.forEach(w -> w.refresh(nomask));
			lastRequestFinished = true;
		});

	}

	public int getGridSize() {
		return gridSize;
	}

	public void setDataRange(int offset, int duration) {
		Map<String, String> lc = InvUtil.getLocationParameters(Window.current().getLocation().getSearch());
		Location.current().setHash("dur=" + duration + "&offset=" + offset);
		JSONObject jo = new JSONObject();
		jo.put("off", offset);
		jo.put("dur", duration);
		jo.put("page", lc.get("layout"));
		post("setParams", jo, res -> widgets.forEach(w -> w.refresh(false)));
	}

	public List<AbstractPageWidget> getWidgets() {
		return widgets;
	}

	public void showConsole() {
		if (console == null)
			console = new ConsolePopup(this, null);
		console.showModal();
	}

}
