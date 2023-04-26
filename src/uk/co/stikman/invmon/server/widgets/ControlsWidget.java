package uk.co.stikman.invmon.server.widgets;

import org.json.JSONObject;

import uk.co.stikman.invmon.server.HTMLBuilder;
import uk.co.stikman.invmon.server.WidgetExecuteContext;

public class ControlsWidget extends PageWidget {
	
	@Override
	public JSONObject execute(JSONObject params, WidgetExecuteContext data) {
		HTMLBuilder html = new HTMLBuilder();
		html.append("Controls");
		JSONObject jo = new JSONObject();
		jo.put("html", html.toString());
		return jo;
	}

	@Override
	public String getClientWidgetType() {
		return "controls";
	}

}
