package uk.co.stikman.invmon.server.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;

import uk.co.stikman.invmon.nanohttpd.NanoHTTPD.Response.Status;
import uk.co.stikman.invmon.server.InvMonHTTPResponse;
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
		if (!api.equals("fetch"))
			throw new RuntimeException("Unknown method");

		JSONObject res = new JSONObject();
		List<String> lst = getOwner().getEnv().copyUserLog(new ArrayList<>());
		res.put("log", lst.stream().collect(Collectors.joining("\n")));
		return res;
	}

	@Override
	public String getClientWidgetType() {
		return "log";
	}

	public String getControllerName() {
		return controllerName;
	}

}
