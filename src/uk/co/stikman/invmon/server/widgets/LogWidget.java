package uk.co.stikman.invmon.server.widgets;

import org.json.JSONObject;

import uk.co.stikman.invmon.server.PageLayout;
import uk.co.stikman.invmon.server.UserSesh;
import uk.co.stikman.log.StikLog;

public class LogWidget extends PageWidget {

	private static final StikLog	LOGGER	= StikLog.getLogger(LogWidget.class);
	private String					controllerName;

	public LogWidget(PageLayout owner) {
		super(owner);
	}

	@Override
	public JSONObject executeApi(UserSesh sesh, String api, JSONObject args) {
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
