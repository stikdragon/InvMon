package uk.co.stikman.invmon.server.widgets;

import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.server.WidgetExecuteContext;
import uk.co.stikman.log.StikLog;

public class TextSummaryWidget extends PageWidget {
	private static final StikLog	LOGGER	= StikLog.getLogger(TextSummaryWidget.class);
	private String					moduleName;

	@Override
	public JSONObject execute(JSONObject params, WidgetExecuteContext ctx) {
		try {
			JSONObject jo = new JSONObject();
			InvModule mod = ctx.getOwner().getEnv().getModule(moduleName);
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
