package uk.co.stikman.invmon.client;

import java.util.function.Consumer;

import org.json.JSONObject;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLElement;

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
			mnu.addItem("Configure...", this::configure);
			mnu.showAt(ev.getClientX(), ev.getClientY());
		}
	}

	protected void showMenu(Event event) {
		event.preventDefault();
	}

	protected void configure() {
		InvMon.INSTANCE.mask();
		api(id, null, null);
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

		return a;
	}

	public String getName() {
		return name;
	}

	protected void api(String api, JSONObject args, Consumer<JSONObject> response) {
		api(api, args, response, ex -> InvMon.getErrorPopup().addMessage("Error: " + ex.getMessage()));
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
