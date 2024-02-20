package uk.co.stikman.invmon.server.widgets;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import uk.co.stikman.invmon.minidom.MDElement;
import uk.co.stikman.invmon.server.InvMonClientError;
import uk.co.stikman.invmon.server.PageLayout;
import uk.co.stikman.invmon.server.UserSesh;
import uk.co.stikman.invmon.shared.OptionEnum;
import uk.co.stikman.invmon.shared.WidgetConfigOptions;
import uk.co.stikman.invmon.stikbms.BatteryData;
import uk.co.stikman.invmon.stikbms.StikBMS;
import uk.co.stikman.log.StikLog;

public class BMSStatusWidget extends PageWidget {

	private static final StikLog	LOGGER	= StikLog.getLogger(BMSStatusWidget.class);
	private String					moduleName;

	public BMSStatusWidget(PageLayout owner) {
		super(owner);
	}

	@Override
	public JSONObject executeApi(UserSesh sesh, String api, JSONObject args) {
		if (api.equals("summary")) {
			JSONObject jo = new JSONObject();
			StikBMS mod = getOwner().getEnv().getModule(moduleName);
			jo.put("summary", mod.toString());
			return jo;
		} else if (api.equals("data")) {
			StikBMS bms = getOwner().getEnv().getModule(moduleName);
			JSONArray arr = new JSONArray();
			List<BatteryData> data = bms.getBatteryData();
			for (BatteryData x : data) {
				JSONArray arr2 = new JSONArray();
				float[] volts = x.getCellVoltages();
				for (int i = 0; i < volts.length; ++i)
					arr2.put(new JSONObject().put("id", i + 1).put("v", volts[i]));
				arr.put(new JSONObject().put("cells", arr2).put("id", x.getId()).put("temp", x.getTemperature()));
			}

			JSONObject jo = new JSONObject();
			jo.put("batteries", arr);
			return jo;
		} else
			throw new InvMonClientError("Unknown Controller API: " + api);
	}

	@Override
	public String getClientWidgetType() {
		return "bms-status";
	}

	@Override
	public void fromDOM(MDElement root) {
		super.fromDOM(root);
		this.moduleName = root.getAttrib("module");
	}

	@Override
	public void toDOM(MDElement root) {
		super.toDOM(root);
		root.setAttrib("module", moduleName);
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
