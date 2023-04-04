package uk.co.stikman.invmon.htmlout;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.inverter.util.InvUtil;

public class ChartWidget extends PageWidget {

	private ChartOptions		opts;
	private String				cssClass;
	private List<HeaderBitDef>	headerBits	= new ArrayList<>();

	@Override
	public void configure(Element root) {
		super.configure(root);
		cssClass = InvUtil.getAttrib(root, "cssClass", null);
		opts = new ChartOptions();

		for (Element el : InvUtil.getElements(root)) {
			if (el.getTagName().equals("Series")) {
				List<String> sub = null;
				if (el.hasAttribute("subfields")) {
					sub = new ArrayList<>();
					for (String s : el.getAttribute("subfields").split(","))
						sub.add(s.trim());
				}
				String s = InvUtil.getAttrib(el, "axis", "y1");
				Series ser = opts.addSeries(InvUtil.getAttrib(el, "field"), sub);
				if (s.equals("y1"))
					ser.setYAxis(opts.getAxisY1());
				else if (s.equals("y2"))
					ser.setYAxis(opts.getAxisY2());
				else
					throw new IllegalArgumentException("Unknown axis: " + s);
			} else if ("Axis".equals(el.getTagName())) {
				Axis ax = null;
				String id = InvUtil.getAttrib(el, "id");
				if (id.equals("x1"))
					ax = opts.getAxisX1();
				else if (id.equals("y1"))
					ax = opts.getAxisY1();
				else if (id.equals("y2"))
					ax = opts.getAxisY2();
				else
					throw new IllegalArgumentException("Unknown axis: " + id);
				if (el.hasAttribute("formatter")) {
					ax.setFormat(el.getAttribute("formatter"));
				}
				if (el.hasAttribute("min"))
					ax.setForceMin(Float.parseFloat(el.getAttribute("min")));
				ax.setEnabled(true);
			} else if ("HeaderBit".equals(el.getTagName())) {
				String special = InvUtil.getAttrib(el, "special", null);
				if (special != null) {
					if (special.equals("powerfactor"))
						headerBits.add(new HeaderBitPF());
					else
						throw new IllegalArgumentException("Unknown special HeaderBit type: " + special);
				} else {
					HeaderBitDef hb = new HeaderBitDef();
					hb.configure(el);
					headerBits.add(hb);
				}
			} else
				throw new IllegalArgumentException("Unexpected element in Widget: " + el.getTagName());
		}

	}

	@Override
	public JSONObject execute(JSONObject params, WidgetExecuteContext qr) {
		opts.setSize(params.getInt("w"), params.getInt("h"));
		HTMLBuilder html = new HTMLBuilder();
		DataSet ds = qr.getResults().asDataSet();

		JSONArray jarr = new JSONArray();
		for (HeaderBitDef hb : headerBits)
			jarr.put(hb.execute(qr));

		JSONObject jo = new JSONObject();
		jo.put("html", html.toString());
		jo.put("titleBits", jarr);
		jo.put("config", opts.toJSON());
		jo.put("css", cssClass);
		jo.put("data", ds.toJSON());
		return jo;
	}

	@Override
	public String getClientWidgetType() {
		return "chart";
	}

	public List<HeaderBitDef> getHeaderBits() {
		return headerBits;
	}

}
