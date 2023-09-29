package uk.co.stikman.invmon.server.widgets;

import org.json.JSONObject;

import uk.co.stikman.invmon.server.WidgetExecuteContext;
import uk.co.stikman.log.StikLog;

public class LogWidget extends PageWidget {
	private static final StikLog	LOGGER	= StikLog.getLogger(LogWidget.class);
	private String					controllerName;

	@Override
	public JSONObject execute(JSONObject params, WidgetExecuteContext ctx) {
		throw new RuntimeException("don't run this");
	}

	@Override
	public String getClientWidgetType() {
		return "log";
	}

	public String getControllerName() {
		return controllerName;
	}

}
