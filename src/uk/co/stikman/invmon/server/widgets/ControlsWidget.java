package uk.co.stikman.invmon.server.widgets;

import org.json.JSONObject;

import uk.co.stikman.invmon.server.InvMonClientError;
import uk.co.stikman.invmon.server.PageLayout;
import uk.co.stikman.invmon.server.UserSesh;
import uk.co.stikman.invmon.shared.WidgetConfigOptions;

public class ControlsWidget extends PageWidget {

	public ControlsWidget(PageLayout owner) {
		super(owner);
	}

	@Override
	public String getClientWidgetType() {
		return "controls";
	}

	@Override
	public JSONObject executeApi(UserSesh sesh, String api, JSONObject args) {
		throw new InvMonClientError("Unknown API");
	}

	@Override
	public WidgetConfigOptions getConfigOptions() {
		WidgetConfigOptions wco = new WidgetConfigOptions();
		return wco;
	}

	@Override
	public void applyConfigOptions(WidgetConfigOptions opts) {
		
	}

}
