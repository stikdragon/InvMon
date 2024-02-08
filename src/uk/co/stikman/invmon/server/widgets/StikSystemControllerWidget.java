package uk.co.stikman.invmon.server.widgets;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.controllers.StikSystemController;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.minidom.MDElement;
import uk.co.stikman.invmon.server.InvMonClientError;
import uk.co.stikman.invmon.server.PageLayout;
import uk.co.stikman.invmon.server.UserRole;
import uk.co.stikman.invmon.server.UserSesh;
import uk.co.stikman.invmon.server.widgets.GaugeWidget.Mode;
import uk.co.stikman.invmon.shared.OptionEnum;
import uk.co.stikman.invmon.shared.OptionFloat;
import uk.co.stikman.invmon.shared.OptionString;
import uk.co.stikman.invmon.shared.OptionType;
import uk.co.stikman.invmon.shared.WidgetConfigOptions;
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
		} else if (api.equals("forceCharge")) {
			sesh.requireUserRole(UserRole.ADMIN);
			StikSystemController mod = getOwner().getEnv().getModule(moduleName);
			try {
				mod.setForceChargeMode(args.getBoolean("state"));
			} catch (Exception e) {
				throw new InvMonClientError("Failed: " + e.getMessage(), e);
			}
			return null;
		}

		return super.executeApi(sesh, api, args);
	}

	@Override
	public String getClientWidgetType() {
		return "stik-controller";
	}

	@Override
	public void configure(MDElement root) throws IllegalArgumentException {
		super.configure(root);
		this.moduleName = root.getAttrib("module");
	}

	public String getModuleName() {
		return moduleName;
	}

	@Override
	public WidgetConfigOptions getConfigOptions() {
		WidgetConfigOptions wco = new WidgetConfigOptions();
		wco.add(new OptionEnum("module", "Target Module", moduleName, getOwner().getEnv().getModuleNames()));
		return wco;
	}

	@Override
	public void applyConfigOptions(WidgetConfigOptions opts) {
		moduleName = opts.get("module", OptionEnum.class).getValue();
	}

}
