package uk.co.stikman.invmon.server.widgets;

import org.json.JSONObject;

import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.minidom.MDElement;
import uk.co.stikman.invmon.server.PageLayout;
import uk.co.stikman.invmon.server.UserSesh;
import uk.co.stikman.invmon.shared.OptionEnum;
import uk.co.stikman.invmon.shared.WidgetConfigOptions;
import uk.co.stikman.log.StikLog;

public class TextSummaryWidget extends PageWidget {

	private static final StikLog	LOGGER	= StikLog.getLogger(TextSummaryWidget.class);
	private String					moduleName;

	public TextSummaryWidget(PageLayout owner) {
		super(owner);
	}

	@Override
	public JSONObject executeApi(UserSesh sesh, String api, JSONObject args) {
		if ("execute".equals(api)) {
			try {
				JSONObject jo = new JSONObject();
				InvModule mod = getOwner().getEnv().getModule(moduleName);
				jo.put("summary", mod.toString());
				return jo;
			} catch (Exception e) {
				LOGGER.error(e);
				throw new RuntimeException("Error rendering summary", e);
			}
		}

		return super.executeApi(sesh, api, args);
	}

	@Override
	public String getClientWidgetType() {
		return "text-summary";
	}

	@Override
	public void fromDOM(MDElement root) throws IllegalArgumentException {
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
