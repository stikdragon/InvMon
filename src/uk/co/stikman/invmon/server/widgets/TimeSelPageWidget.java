package uk.co.stikman.invmon.server.widgets;

import org.json.JSONObject;

import uk.co.stikman.invmon.server.WidgetExecuteContext;

public class TimeSelPageWidget extends PageWidget {
	@Override
	public JSONObject execute(JSONObject params, WidgetExecuteContext qr) {
		return new JSONObject();
	}

	@Override
	public String getClientWidgetType() {
		return "timesel";
	}

}