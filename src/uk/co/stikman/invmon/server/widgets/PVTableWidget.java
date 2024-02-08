package uk.co.stikman.invmon.server.widgets;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.minidom.MDElement;
import uk.co.stikman.invmon.server.HTMLBuilder;
import uk.co.stikman.invmon.server.PageLayout;
import uk.co.stikman.invmon.server.UserSesh;
import uk.co.stikman.invmon.server.WebUtils;
import uk.co.stikman.invmon.server.widgets.GaugeWidget.Mode;
import uk.co.stikman.invmon.shared.OptionEnum;
import uk.co.stikman.invmon.shared.OptionFloat;
import uk.co.stikman.invmon.shared.OptionString;
import uk.co.stikman.invmon.shared.OptionStringList;
import uk.co.stikman.invmon.shared.OptionType;
import uk.co.stikman.invmon.shared.WidgetConfigOptions;

public class PVTableWidget extends PageWidget {

	private List<String>	fieldNames		= new ArrayList<>();
	private List<String>	descriptions	= new ArrayList<>();

	public PVTableWidget(PageLayout owner) {
		super(owner);
	}

	@Override
	public JSONObject executeApi(UserSesh sesh, String api, JSONObject args) {
		if ("execute".equals(api)) {
			ensureCachedResults(sesh);
			DBRecord rec = sesh.getData(WebUtils.CACHED_LAST_RECORD);
			HTMLBuilder html = new HTMLBuilder();
			DataModel mdl = getOwner().getEnv().getModel();
			html.append("<table class=\"data\">");
			html.append("<tr><td></td><th>P</th><th>V</th><th>I</th></tr>");
			for (int i = 0; i < fieldNames.size(); ++i) {
				html.append("<tr>");
				html.append("<th>").append(descriptions.get(i)).append("</th>");
				html.append("<td><span class=\"b\">").append((int) rec.getFloat(mdl.get(fieldNames.get(i) + "_P"))).append("</span>W</td>");
				html.append("<td><span class=\"b\">").append((int) rec.getFloat(mdl.get(fieldNames.get(i) + "_V"))).append("</span>V</td>");
				html.append(String.format("<td><span class=\"b\">%.1f</span>A</td>", rec.getFloat(mdl.get(fieldNames.get(i) + "_I"))));
				html.append("</tr>");
			}

			html.append("<tr class=\"total\"><th>Total</th><td colspan=\"3\"><span class=\"b\">").append((int) rec.getFloat(mdl.get("PV_TOTAL_P"))).append("</span>W</td></tr>");
			html.append("</table>");
			JSONObject jo = new JSONObject();
			jo.put("html", html.toString());
			return jo;
		}

		return super.executeApi(sesh, api, args);
	}

	@Override
	public String getClientWidgetType() {
		return "serverside";
	}

	@Override
	public void configure(MDElement root) {
		super.configure(root);
		String fields = root.getAttrib("fields");
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

	@Override
	public WidgetConfigOptions getConfigOptions() {
		WidgetConfigOptions wco = new WidgetConfigOptions();
		wco.add(new OptionStringList("fields", "Fields", fieldNames));
		wco.add(new OptionStringList("desc", "Descriptions", descriptions));
		return wco;
	}

	@Override
	public void applyConfigOptions(WidgetConfigOptions opts) {
		fieldNames = new ArrayList<>();
		descriptions = new ArrayList<>();
		fieldNames.addAll(opts.get("fields", OptionStringList.class).getValue());
		descriptions.addAll(opts.get("desc", OptionStringList.class).getValue());
	}

}
