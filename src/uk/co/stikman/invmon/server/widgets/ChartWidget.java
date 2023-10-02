package uk.co.stikman.invmon.server.widgets;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.server.Axis;
import uk.co.stikman.invmon.server.ChartOptions;
import uk.co.stikman.invmon.server.DataSet;
import uk.co.stikman.invmon.server.HTMLBuilder;
import uk.co.stikman.invmon.server.HeaderBitDef;
import uk.co.stikman.invmon.server.HeaderBitPF;
import uk.co.stikman.invmon.server.PageLayout;
import uk.co.stikman.invmon.server.Series;
import uk.co.stikman.invmon.server.UserSesh;

public class ChartWidget extends PageWidget {

	private ChartOptions		opts;
	private String				cssClass;
	private List<HeaderBitDef>	headerBits	= new ArrayList<>();

	public ChartWidget(PageLayout owner) {
		super(owner);
	}

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
	public JSONObject executeApi(UserSesh sesh, String api, JSONObject args) {
		//
		// make sure we've got the current cached result set for this session
		//
		ensureCachedResults(sesh);
		QueryResults qr = sesh.getData(CACHED_QUERY_RESULTS);
		DBRecord lastrec = sesh.getData(CACHED_LAST_RECORD);

		opts.setSize(args.getInt("w"), args.getInt("h"));
		HTMLBuilder html = new HTMLBuilder();
		DataSet ds = qr.asDataSet();

		JSONArray jarr = new JSONArray();
		for (HeaderBitDef hb : headerBits)
			jarr.put(hb.execute(getOwner().getEnv(), lastrec));

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
