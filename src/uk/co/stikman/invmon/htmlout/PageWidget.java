package uk.co.stikman.invmon.htmlout;

import java.util.Collection;

import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.inverter.util.InvUtil;

public abstract class PageWidget {
	private String	id;
	private int		x;
	private int		y;
	private int		width;
	private int		height;
	private String	title;

	public String getId() {
		return id;
	}

	public void configure(Element root) {
		id = InvUtil.getAttrib(root, "id");
		String s = InvUtil.getAttrib(root, "layout");
		String[] bits = s.split(",");
		x = Integer.parseInt(bits[0].trim());
		y = Integer.parseInt(bits[1].trim());
		width = Integer.parseInt(bits[2].trim());
		height = Integer.parseInt(bits[3].trim());
		title = InvUtil.getAttrib(root, "title", null);

	}

	public abstract JSONObject execute(JSONObject params, QueryResults qr);

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

	public abstract String getWidgetType();

	public String getTitle() {
		return title;
	}

}
