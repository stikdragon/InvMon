package uk.co.stikman.invmon.htmlout;

import org.json.JSONObject;

import uk.co.stikman.invmon.datalog.QueryResults;

public class ControlsWidget extends PageWidget {
	
	@Override
	public JSONObject execute(JSONObject params, QueryResults data) {
		HTMLBuilder html = new HTMLBuilder();
		html.append("Controls");
		JSONObject jo = new JSONObject();
		jo.put("html", html.toString());
		return jo;
	}

	@Override
	public String getWidgetType() {
		return "controls";
	}

}
