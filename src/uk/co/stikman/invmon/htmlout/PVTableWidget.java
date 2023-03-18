package uk.co.stikman.invmon.htmlout;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.datalog.QueryRecord;
import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.inverter.util.InvUtil;

public class PVTableWidget extends PageWidget {
	private List<String>	fieldNames		= new ArrayList<>();
	private List<String>	descriptions	= new ArrayList<>();

	@Override
	public JSONObject execute(JSONObject params, QueryResults data) {
		QueryRecord rec = data.getRecords().get(data.getRecords().size() - 1);
		HTMLBuilder html = new HTMLBuilder();
		html.append("<table class=\"data\">");
		html.append("<tr><td></td><th>P</th><th>V</th><th>I</th></tr>");
		for (int i = 0; i < fieldNames.size(); ++i) {
			html.append("<tr>");
			html.append("<th>").append(descriptions.get(i)).append("</th>");
			html.append("<td><span class=\"b\">").append((int) rec.getFloat(fieldNames.get(i) + "_P")).append("</span>W</td>");
			html.append("<td><span class=\"b\">").append((int) rec.getFloat(fieldNames.get(i) + "_V")).append("</span>V</td>");
			html.append(String.format("<td><span class=\"b\">%.1f</span>A</td>", rec.getFloat(fieldNames.get(i) + "_I")));
			html.append("</tr>");
		}

		html.append("<tr class=\"total\"><th>Total</th><td colspan=\"3\"><span class=\"b\">").append((int) rec.getFloat("PV_TOTAL_P")).append("</span>W</td></tr>");
		html.append("</table>");
		JSONObject jo = new JSONObject();
		jo.put("html", html.toString());
		return jo;
	}

	@Override
	public String getWidgetType() {
		return "chart";
	}

	@Override
	public void configure(Element root) {
		super.configure(root);
		String fields = InvUtil.getAttrib(root, "fields");
		for (String fld : fields.split(",")) {
			String name = fld;
			int pos = fld.indexOf(':');
			if (pos != -1) {
				name = fld.substring(pos + 1);
				fld = fld.substring(0, pos);
			}

			fieldNames.add(fld);
			descriptions.add(name);
		}
	}

}
