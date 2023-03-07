package uk.co.stikman.invmon.htmlout;

import java.util.Date;

import org.json.JSONObject;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.datalog.QueryResults;

public class InfoBitWidget extends PageWidget {

	@Override
	public JSONObject execute(JSONObject params, QueryResults qr) {
		HTMLBuilder html = new HTMLBuilder();
		html.append("<div class=\"tiny\"><div class=\"a\">Local Time: </div><div class=\"b\">").append(new Date().toString()).append("</div></div>");
		html.append("<div class=\"tiny\"><div class=\"a\">Version: </div><div class=\"b\">").append(Env.getVersion()).append("</div></div>");
		JSONObject jo = new JSONObject();
		jo.put("html", html.toString());
		return jo;
	}

	@Override
	public String getWidgetType() {
		return "infobit";
	}

}
