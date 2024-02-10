package uk.co.stikman.invmon.server.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.minidom.MDElement;
import uk.co.stikman.invmon.server.Axis;
import uk.co.stikman.invmon.server.ChartOptions;
import uk.co.stikman.invmon.server.DataSet;
import uk.co.stikman.invmon.server.HTMLBuilder;
import uk.co.stikman.invmon.server.HeaderBitDef;
import uk.co.stikman.invmon.server.HeaderBitPF;
import uk.co.stikman.invmon.server.PageLayout;
import uk.co.stikman.invmon.server.Series;
import uk.co.stikman.invmon.server.UserSesh;
import uk.co.stikman.invmon.server.WebUtils;
import uk.co.stikman.invmon.shared.OptionString;
import uk.co.stikman.invmon.shared.WidgetConfigOptions;

public class ChartWidget extends PageWidget {

	private ChartOptions		opts;
	private String				cssClass;
	private List<HeaderBitDef>	headerBits	= new ArrayList<>();

	public ChartWidget(PageLayout owner) {
		super(owner);
	}

	@Override
	public void fromDOM(MDElement root) {
		super.fromDOM(root);
		cssClass = root.getAttrib("cssClass", null);
		opts = new ChartOptions();

		for (MDElement el : root) {
			if (el.getName().equals("Series")) {
				List<String> sub = null;
				if (el.hasAttrib("subfields")) {
					sub = new ArrayList<>();
					for (String s : el.getAttrib("subfields").split(","))
						sub.add(s.trim());
				}
				String s = el.getAttrib("axis", "y1");
				Series ser = opts.addSeries(el.getAttrib("field"), sub);
				if (s.equals("y1"))
					ser.setYAxis(opts.getAxisY1());
				else if (s.equals("y2"))
					ser.setYAxis(opts.getAxisY2());
				else
					throw new IllegalArgumentException("Unknown axis: " + s);
			} else if ("Axis".equals(el.getName())) {
				Axis ax = null;
				String id = el.getAttrib("id");
				if (id.equals("x1"))
					ax = opts.getAxisX1();
				else if (id.equals("y1"))
					ax = opts.getAxisY1();
				else if (id.equals("y2"))
					ax = opts.getAxisY2();
				else
					throw new IllegalArgumentException("Unknown axis: " + id);
				if (el.hasAttrib("formatter")) {
					ax.setFormat(el.getAttrib("formatter"));
				}
				if (el.hasAttrib("min"))
					ax.setForceMin(Float.parseFloat(el.getAttrib("min")));
				if (el.hasAttrib("max"))
					ax.setForceMax(Float.parseFloat(el.getAttrib("max")));
				ax.setEnabled(true);
			} else if ("HeaderBit".equals(el.getName())) {
				String special = el.getAttrib("special", null);
				if (special != null) {
					if (special.equals("powerfactor"))
						headerBits.add(new HeaderBitPF());
					else
						throw new IllegalArgumentException("Unknown special HeaderBit type: " + special);
				} else {
					HeaderBitDef hb = new HeaderBitDef();
					hb.fromDOM(el);
					headerBits.add(hb);
				}
			} else
				throw new IllegalArgumentException("Unexpected element in Widget: " + el.getName());
		}

	}

	@Override
	public void toDOM(MDElement root) {
		super.toDOM(root);

		root.setAttrib("cssClass", cssClass);
		for (Series ser : opts.getSeries()) {
			MDElement el = root.add("Series");
			el.setAttrib("field", ser.getField());
			String s = ser.getSubfields().stream().collect(Collectors.joining(","));
			if (!s.isEmpty())
				el.setAttrib("subfields", s);
			if (ser.getYAxisId() == opts.getAxisY1().getId())
				el.setAttrib("axis", "y1");
			else if (ser.getYAxisId() == opts.getAxisY2().getId())
				el.setAttrib("axis", "y2");
		}

		writeAxisToDom(root, "x1", opts.getAxisX1());
		writeAxisToDom(root, "y1", opts.getAxisY1());
		writeAxisToDom(root, "y2", opts.getAxisY2());

		for (HeaderBitDef hb : headerBits) {
			MDElement el = root.add("HeaderBit");
			// TODO: i don't like this bit
			if (hb instanceof HeaderBitPF)
				el.setAttrib("special", "powerfactor");
			else
				hb.toDOM(el);
		}

	}

	private void writeAxisToDom(MDElement root, String id, Axis ax) {
		MDElement el = root.add("Axis");
		el.setAttrib("id", id);
		el.setAttrib("formatter", ax.getFormat());
		if (ax.getForceMin() != null)
			el.setAttrib("min", ax.getForceMin().floatValue());
		if (ax.getForceMax() != null)
			el.setAttrib("max", ax.getForceMax());
		el.setAttrib("enabled", ax.isEnabled());
	}

	@Override
	public JSONObject executeApi(UserSesh sesh, String api, JSONObject args) {
		if ("execute".equals(api)) {
			//
			// make sure we've got the current cached result set for this session
			//
			ensureCachedResults(sesh);
			QueryResults qr = sesh.getData(WebUtils.CACHED_QUERY_RESULTS);
			DBRecord lastrec = sesh.getData(WebUtils.CACHED_LAST_RECORD);

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

		return super.executeApi(sesh, api, args);
	}

	@Override
	public String getClientWidgetType() {
		return "chart";
	}

	public List<HeaderBitDef> getHeaderBits() {
		return headerBits;
	}

	@Override
	public WidgetConfigOptions getConfigOptions() {
		WidgetConfigOptions wco = new WidgetConfigOptions();
		wco.add(new OptionString("todo", "Chart Options", "this is not implemented yet"));
		return wco;
	}

	@Override
	public void applyConfigOptions(WidgetConfigOptions opts) {
	}

}
