package uk.co.stikman.invmon.server.widgets;

import org.json.JSONObject;

import uk.co.stikman.invmon.server.InvMonClientError;
import uk.co.stikman.invmon.server.PageLayout;
import uk.co.stikman.invmon.server.UserSesh;
import uk.co.stikman.invmon.server.ViewOptions;

public class TimeSelPageWidget extends PageWidget {
	public TimeSelPageWidget(PageLayout owner) {
		super(owner);
	}

	@Override
	public String getClientWidgetType() {
		return "timesel";
	}

	@Override
	public JSONObject executeApi(UserSesh sesh, String api, JSONObject args) {
		if (!api.equals("setParams"))
			throw new InvMonClientError("Unknown API");

		int dur = args.getInt("dur");
		int off = args.getInt("off");
		ViewOptions global = PageWidget.getViewOpts(sesh);
		global.setDuration(dur);
		global.setOffset(off);
		//		global.setLayout(this.layoutConfig.getPage(jo.optString("page", null)));
		sesh.putData(CACHED_QUERY_RESULTS, null);
		sesh.putData(CACHED_LAST_RECORD, null);
		return new JSONObject();
	}

}