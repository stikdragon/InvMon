package uk.co.stikman.invmon.client;

import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;

public abstract class PageWidget {

	private final ClientPage	owner;
	private int					x;
	private int					y;
	private int					width;
	private int					height;
	private String				id;

	public PageWidget(ClientPage owner) {
		super();
		this.owner = owner;
	}

	public void configure(JSONObject obj) {
		x = obj.getInt("x");
		y = obj.getInt("y");
		width = obj.getInt("w");
		height = obj.getInt("h");
		id = obj.getString("id");
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

	protected abstract void refresh();

	protected abstract void construct(HTMLElement parent);

	public ClientPage getOwner() {
		return owner;
	}

	protected StandardFrame createStandardFrame(HTMLElement parent, boolean header, String mainclass) {
		HTMLElement root = InvMon.div();
		if (mainclass != null)
			root.getClassList().add(mainclass);
		parent.appendChild(root);
		root.getClassList().add("gridframe");
		doLayout(root);

		HTMLElement inner = InvMon.div("gridframeinner");
		root.appendChild(inner);

		HTMLElement hdr = null;
		if (header) {
			hdr = InvMon.div("hdr");
			inner.appendChild(hdr);
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

		StandardFrame a = new StandardFrame();
		a.header = hdr;
		a.content = el2;
		a.glass = elGlass;
		a.error = elError;
		a.hideOverlays();
		return a;
	}

}
