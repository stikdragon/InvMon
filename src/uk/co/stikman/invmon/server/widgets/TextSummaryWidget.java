package uk.co.stikman.invmon.server.widgets;

import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.server.PageLayout;
import uk.co.stikman.invmon.server.UserSesh;
import uk.co.stikman.log.StikLog;

public class TextSummaryWidget extends PageWidget {

	private static final StikLog	LOGGER	= StikLog.getLogger(TextSummaryWidget.class);
	private String					moduleName;

	public TextSummaryWidget(PageLayout owner) {
		super(owner);
	}

	@Override
	public JSONObject executeApi(UserSesh sesh, String api, JSONObject args) {
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

	@Override
	public String getClientWidgetType() {
		return "text-summary";
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
