package uk.co.stikman.invmon.htmlout;

import org.json.JSONObject;

import uk.co.stikman.invmon.datalog.QueryRecord;
import uk.co.stikman.invmon.datalog.QueryResults;

public class TableWidget extends PageWidget {
	private static final String[] IDS = ",PVA_1,PVB_1,PVA_2,PVB_2".split(",");

	@Override
	public JSONObject execute(JSONObject params, QueryResults data) {
		QueryRecord rec = data.getRecords().get(data.getRecords().size() - 1);
		HTMLBuilder html = new HTMLBuilder();
		html.append("<table class=\"data\">");
		html.append("<tr><td></td><th>P</th><th>V</th><th>I</th></tr>");
		for (int i = 1; i <= 4; ++i) {
			html.append("<tr>");
			html.append("<th>").append(IDS[i]).append("</th>");
			html.append("<td><span class=\"b\">").append((int) rec.getFloat(IDS[i] + "_P")).append("</span>W</td>");
			html.append("<td><span class=\"b\">").append((int) rec.getFloat(IDS[i] + "_V")).append("</span>V</td>");
			html.append(String.format("<td><span class=\"b\">%.1f</span>A</td>", rec.getFloat(IDS[i] + "_I")));
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

}
