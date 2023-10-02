package uk.co.stikman.invmon.server.widgets;

import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.controllers.StikSystemController;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.server.InvMonClientError;
import uk.co.stikman.invmon.server.PageLayout;
import uk.co.stikman.invmon.server.UserRole;
import uk.co.stikman.invmon.server.UserSesh;
import uk.co.stikman.log.StikLog;

public class StikSystemControllerWidget extends PageWidget {

	private static final StikLog	LOGGER	= StikLog.getLogger(TextSummaryWidget.class);
	private String					moduleName;

	public StikSystemControllerWidget(PageLayout owner) {
		super(owner);
	}

	@Override
	public JSONObject executeApi(UserSesh sesh, String api, JSONObject args) {
		if (api.equals("summary")) {
			JSONObject jo = new JSONObject();
			StikSystemController mod = getOwner().getEnv().getModule(moduleName);
			jo.put("summary", mod.toString());
			return jo;
		} else if (api.equals("setBoost")) {
			sesh.requireUserRole(UserRole.ADMIN);

			int dur = args.getInt("duration");
			StikSystemController mod = getOwner().getEnv().getModule(moduleName);
			mod.setBoost(dur);
			return null;
		} else
			throw new InvMonClientError("Unknown Controller API: " + api);
	}

	@Override
	public String getClientWidgetType() {
		return "stik-controller";
	}

	@Override
	public void configure(Element root) throws IllegalArgumentException {
		super.configure(root);
		this.moduleName = InvUtil.getAttrib(root, "module");
	}

	public String getModuleName() {
		return moduleName;
	}

}
