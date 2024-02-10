package uk.co.stikman.invmon.server.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;

import uk.co.stikman.invmon.server.PageLayout;
import uk.co.stikman.invmon.server.UserSesh;
import uk.co.stikman.invmon.shared.WidgetConfigOptions;

public class LogWidget extends PageWidget {

	public LogWidget(PageLayout owner) {
		super(owner);
	}

	@Override
	public JSONObject executeApi(UserSesh sesh, String api, JSONObject args) {
		if (api.equals("fetch")) {

			JSONObject res = new JSONObject();
			List<String> lst = getOwner().getEnv().copyUserLog(new ArrayList<>());
			res.put("log", lst.stream().collect(Collectors.joining("\n")));
			return res;
		}

		return super.executeApi(sesh, api, args);
	}

	@Override
	public String getClientWidgetType() {
		return "log";
	}

	@Override
	public WidgetConfigOptions getConfigOptions() {
		return new WidgetConfigOptions();
	}

	@Override
	public void applyConfigOptions(WidgetConfigOptions opts) {
	}

}
