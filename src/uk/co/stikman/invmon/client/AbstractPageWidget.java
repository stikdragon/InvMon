package uk.co.stikman.invmon.client;

import java.util.function.Consumer;

import org.json.JSONObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.client.MessagePopup.Type;
import uk.co.stikman.invmon.shared.WidgetConfigOptions;

public abstract class AbstractPageWidget {

	private final ClientPage	owner;
	private int					x;
	private int					y;
	private int					width;
	private int					height;
	private String				id;
	private String				name;
	private HTMLElement			root;
	private HTMLElement			resizehandle;

	//
	// support for dragging them around
	//
	private int					startY;
	private int					startX;
	private int					startH;
	private int					startW;
	private int					lastTimeout		= -1;

	private static int			zIndexCounter	= 0;

	public AbstractPageWidget(ClientPage owner) {
		super();
		this.owner = owner;
	}

	public void configure(JSONObject obj) {
		x = obj.getInt("x");
		y = obj.getInt("y");
		width = obj.getInt("w");
		height = obj.getInt("h");
		id = obj.getString("id");
		name = obj.optString("title", null);
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return id;
	}

	protected void doLayout(HTMLElement el) {
		el.getStyle().setProperty("display", "inline-block");
		el.getStyle().setProperty("position", "absolute");
		el.getStyle().setProperty("top", Integer.toString(y) + "px");
		el.getStyle().setProperty("left", Integer.toString(x) + "px");
		el.getStyle().setProperty("width", Integer.toString(width) + "px");
		el.getStyle().setProperty("height", Integer.toString(height) + "px");
	}

	protected abstract void refresh(boolean nomask);

	protected abstract void construct(HTMLElement parent);

	public ClientPage getOwner() {
		return owner;
	}

	protected void mouseClick(Event event) {
		MouseEvent ev = event.cast();

	}

	protected void showContextMenu(Event event) {
		MouseEvent ev = event.cast();

		if (ev.getButton() == MouseEvent.RIGHT_BUTTON) {
			ev.stopPropagation();
			ev.preventDefault();
			Menu mnu = new Menu();
			mnu.addItem("Configure...", this::doConfigureDialog);

			Window window = Window.current();
			//			int offx = window.getScrollX(); // missing atm
			int offy = window.getScrollY();
			mnu.showAt(ev.getClientX(), ev.getClientY() + offy);
		}
	}

	protected void showMenu(Event event) {
		event.preventDefault();
	}

	protected void doConfigureDialog() {
		InvMon.INSTANCE.mask();
		api("getConfig", null, res -> {
			InvMon.INSTANCE.unmask();
			WidgetConfigOptions co = new WidgetConfigOptions();
			co.fromJSON(res);
			ConfigPopupWindow wnd = new ConfigPopupWindow(getOwner(), co, ok -> {
				JSONObject jo = new JSONObject();
				ok.toJSON(jo);
				getOwner().getBus().fire(Events.REFRESH_NOW, null);
				api("setConfig", jo, v -> reload());
			});
			wnd.showModal();
			
		}, err -> {
			InvMon.INSTANCE.unmask();
			InvMon.getMessagePopup().addMessage(err.getMessage(), Type.ERROR);
		});
	}

	private void reload() {

	}

	protected StandardFrame createStandardFrame(HTMLElement parent, boolean header, String mainclass) {
		root = InvMon.div();
		if (mainclass != null)
			root.getClassList().add(mainclass);
		parent.appendChild(root);
		root.getClassList().add("gridframe");
		root.setId(getId());
		doLayout(root);

		HTMLElement inner = InvMon.div("gridframeinner");
		root.appendChild(inner);

		StandardFrame a = new StandardFrame(header);
		if (header) {
			inner.appendChild(a.getHeader());
			if (name != null)
				a.setTitle(name);
			DragHelper dh = new DragHelper(a.getHeader());
			dh.setDragStartHandler(() -> {
				startX = x;
				startY = y;
			});
			dh.setDragHandler((nx, ny) -> {
				nx += startX;
				ny += startY;
				int gs = ((StandardPage) owner).getGridSize();
				x = gs * (int) (nx / gs);
				y = gs * (int) (ny / gs);
				layoutUpdated();
				doLayout(root);
			});

		}

		HTMLElement el2 = InvMon.div("content");
		inner.appendChild(el2);

		HTMLElement elGlass = InvMon.div("glass");
		HTMLElement img = InvMon.element("img");
		img.setAttribute("src", "loading.gif");
		elGlass.appendChild(img);
		inner.appendChild(elGlass);

		HTMLElement elError = InvMon.div("error");
		HTMLElement elMsg = InvMon.div("message");
		elError.appendChild(elMsg);
		inner.appendChild(elError);

		if (header) {
			resizehandle = InvMon.div("resizehandle");
			inner.appendChild(resizehandle);
			DragHelper dh = new DragHelper(resizehandle);
			dh.setDragStartHandler(() -> {
				startW = width;
				startH = height;
			});
			dh.setDragHandler((nx, ny) -> {
				nx += startW;
				ny += startH;
				int gs = ((StandardPage) owner).getGridSize();
				width = gs * (int) (nx / gs);
				height = gs * (int) (ny / gs);
				layoutUpdated();
				doLayout(root);
			});

		}

		a.content = el2;
		a.glass = elGlass;
		a.error = elError;
		a.hideOverlays();

		root.addEventListener("contextmenu", this::showMenu);
		root.addEventListener("mousedown", ev -> root.getStyle().setProperty("z-index", Integer.toString(++zIndexCounter)));
		if (header)
			a.getHeader().addEventListener("mousedown", this::showContextMenu);
		else
			root.addEventListener("mousedown", this::showContextMenu);
		root.addEventListener("click", this::mouseClick);
		elError.addEventListener("mousedown", this::showContextMenu); // add it to the error mask too, so you can configure something you broke

		return a;
	}

	/**
	 * we'll set a timeout to send this back to the server, to avoid spamming it
	 */
	private void layoutUpdated() {
		if (lastTimeout != -1)
			Window.clearTimeout(lastTimeout);
		lastTimeout = Window.setTimeout(() -> {
			lastTimeout = -1;
			JSONObject jo = new JSONObject();
			int gs = ((StandardPage) owner).getGridSize();
			jo.put("x", x / gs);
			jo.put("y", y / gs);
			jo.put("w", width / gs);
			jo.put("h", height / gs);
			api("setPosition", jo, v -> {
			});
		}, 500);
	}

	public String getName() {
		return name;
	}

	protected void api(String api, JSONObject args, Consumer<JSONObject> response) {
		api(api, args, response, ex -> InvMon.getMessagePopup().addMessage("Error: " + ex.getMessage(), Type.ERROR));
	}

	protected void api(String api, JSONObject args, Consumer<JSONObject> response, Consumer<Exception> error) {
		JSONObject jo = new JSONObject();
		jo.put("id", getId());
		jo.put("api", api);
		jo.put("args", args);
		//
		// api errors are wrapped up in JSON responses
		//
		getOwner().post("api", jo, response, errmsg -> {
			JSONObject jo2 = new JSONObject(errmsg);
			String msg = jo2.getString("error");
			error.accept(new RPCError(msg));
		});
	}
}
