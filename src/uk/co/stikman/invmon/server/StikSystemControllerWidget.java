package uk.co.stikman.invmon.server;

import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.controllers.StikSystemController;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.server.widgets.PageWidget;
import uk.co.stikman.invmon.server.widgets.TextSummaryWidget;
import uk.co.stikman.log.StikLog;

public class StikSystemControllerWidget extends PageWidget {

	private static final StikLog	LOGGER	= StikLog.getLogger(TextSummaryWidget.class);
	private String					moduleName;

	public StikSystemControllerWidget(PageLayout owner) {
		super(owner);
	}

	@Override
	public JSONObject executeApi(UserSesh sesh, String api, JSONObject args) {
		try {
			JSONObject jo = new JSONObject();
			StikSystemController mod = getOwner().getEnv().getModule(moduleName);
			jo.put("summary", mod.toString());
			return jo;
		} catch (Exception e) {
			LOGGER.error(e);
			throw new RuntimeException("Error rendering summary", e);
		}
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
