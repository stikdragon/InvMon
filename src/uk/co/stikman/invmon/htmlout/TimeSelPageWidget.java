package uk.co.stikman.invmon.htmlout;

import org.json.JSONObject;

import uk.co.stikman.invmon.datalog.QueryResults;

class TimeSelPageWidget extends PageWidget {
	@Override
	public JSONObject execute(JSONObject params, QueryResults qr) {
		return new JSONObject();
	}

	@Override
	public String getWidgetType() {
		return "timesel";
	}

}