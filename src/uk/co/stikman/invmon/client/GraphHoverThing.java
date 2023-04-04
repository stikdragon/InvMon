package uk.co.stikman.invmon.client;

import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Element;

import uk.co.stikman.invmon.htmlout.Axis;
import uk.co.stikman.invmon.htmlout.ChartOptions;
import uk.co.stikman.invmon.htmlout.HTMLBuilder;

public class GraphHoverThing {
	private class Marker {
		Element			elVert;
		Element			elHoriz;
		HTMLElement		elText;
		String			display;
		int				posX;
		int				posY;
		private boolean	hidden	= true;

		public Marker() {
			elVert = InvMon.createSvgElement("path");
			elHoriz = InvMon.createSvgElement("path");
			elText = InvMon.div("hovertext");
			elVert.setAttribute("class", "crosshair");
			elHoriz.setAttribute("class", "crosshair");
			elText.getStyle().setProperty("position", "absolute");
		}

		public void show() {
			if (!hidden)
				return;
			svg.appendChild(elVert);
			svg.appendChild(elHoriz);
			root.appendChild(elText);
			hidden = false;
		}

		public void hide() {
			if (hidden)
				return;
			svg.removeChild(elHoriz);
			svg.removeChild(elVert);
			root.removeChild(elText);
			hidden = true;
		}

		public void setXY(int x, int y, float sidex, float sidey, String html) {
			show();
			elVert.setAttribute("d", String.format("M%d %d L%d %d", x, 0, x, h));
			elHoriz.setAttribute("d", String.format("M%d %d L%d %d", 0, y, w, y));

			if (sidex > 0)
				x += 10;
			else
				x -= elText.getOffsetWidth() + 10;
			if (sidey > 0)
				y += 10;
			else
				y -= elText.getOffsetHeight() + 10;

			elText.getStyle().setProperty("left", x + "px");
			elText.getStyle().setProperty("top", y + "px");
			elText.setInnerHTML(html);
		}

	}

	private HTMLElement		root;
	private int				x0;
	private int				y0;
	private int				h;
	private int				w;

	private Marker			hoverMarker;
	private Element			svg;
	private ChartOptions	info;

	public GraphHoverThing(int x0, int y0, int w, int h, ChartOptions opts) {
		this.info = opts;
		this.root = InvMon.div("graphhover");
		this.svg = InvMon.createSvgElement("svg");
		root.appendChild(svg);
		svg.setAttribute("width", Integer.toString(w) + "px");
		svg.setAttribute("height", Integer.toString(h) + "px");
		this.x0 = x0;
		this.y0 = y0;
		this.w = w;
		this.h = h;
		root.getStyle().setProperty("position", "absolute");
		root.getStyle().setProperty("left", Integer.toString(x0) + "px");
		root.getStyle().setProperty("top", Integer.toString(y0) + "px");
		root.getStyle().setProperty("width", Integer.toString(w) + "px");
		root.getStyle().setProperty("height", Integer.toString(h) + "px");

		hoverMarker = new Marker();

		root.addEventListener("mousemove", ev -> {
			MouseEvent ev2 = ev.cast();
			int x = ev2.getOffsetX();
			int y = ev2.getOffsetY();
			HTMLBuilder html = new HTMLBuilder();

			doAx(opts.getAxisX1(), x, w, html);
			doAx(opts.getAxisY1(), h - y, h, html);
			if (opts.getAxisY2() != null)
				doAx(opts.getAxisY2(), y, h, html);

			int dx = -1;
			int dy = -1;
			if ((float) x / w < 0.5)
				dx = 1;
			if ((float) y / h < 0.5)
				dy = 1;

			hoverMarker.setXY(x, y, dx, dy, html.toString());
		});

		root.addEventListener("mouseout", ev -> {
			hoverMarker.hide();
		});

	}

	private void doAx(Axis axis, float val, float screen, HTMLBuilder html) {
		float a = axis.getMax() - axis.getMin();
		val /= screen; // normalised
		val *= a; // data
		val += axis.getMin(); // offset
		html.div("row").div("k").append(axis.getName()).append(": </div>");

		html.div("v").append(axis.getFormatter().apply(Float.valueOf(val))).append("</div></div>");
	}

	public HTMLElement getElement() {
		return root;
	}

}
