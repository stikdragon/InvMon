package uk.co.stikman.invmon.client;

import org.json.JSONObject;
import org.teavm.jso.dom.events.EventListener;
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
	private HTMLElement			hdr;
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

	protected StandardFrame createStandardFrame(HTMLElement parent, boolean header, String mainclass) {
		root = InvMon.div();
		if (mainclass != null)
			root.getClassList().add(mainclass);
		parent.appendChild(root);
		root.getClassList().add("gridframe");
		root.setId(getId());
		root.addEventListener("mousedown", ev -> root.getStyle().setProperty("z-index", Integer.toString(++zIndexCounter)));
		doLayout(root);

		HTMLElement inner = InvMon.div("gridframeinner");
		root.appendChild(inner);

		hdr = null;
		if (header) {
			hdr = InvMon.div("hdr");
			inner.appendChild(hdr);
			if (name != null) {
				HTMLElement h1 = InvMon.element("h1", "title");
				h1.setInnerText(name);
				hdr.appendChild(h1);
			}
			DragHelper dh = new DragHelper(hdr);
			dh.setDragStartHandler(() -> {
				startX = x;
				startY = y;
			});
			dh.setDragHandler((nx, ny) -> {
				nx += startX;
				ny += startY;
				int gs = ((MainPage) owner).getGridSize();
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
				int gs = ((MainPage) owner).getGridSize();
				width = gs * (int) (nx / gs);
				height = gs * (int) (ny / gs);
				doLayout(root);
			});

		}

		StandardFrame a = new StandardFrame();
		a.header = hdr;
		a.content = el2;
		a.glass = elGlass;
		a.error = elError;
		a.hideOverlays();
		return a;
	}

	public String getName() {
		return name;
	}

}
