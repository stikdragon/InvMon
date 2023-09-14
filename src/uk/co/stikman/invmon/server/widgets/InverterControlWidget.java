package uk.co.stikman.invmon.server.widgets;

import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.controllers.InverterController;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.server.WidgetExecuteContext;
import uk.co.stikman.log.StikLog;

public class InverterControlWidget extends PageWidget {
	private static final StikLog	LOGGER	= StikLog.getLogger(InverterControlWidget.class);
	private String					controllerName;

	@Override
	public JSONObject execute(JSONObject params, WidgetExecuteContext ctx) {
		try {
			JSONObject jo = new JSONObject();
			InverterController mod = ctx.getOwner().getEnv().getModule(controllerName);
			jo.put("summary", mod.toString());
			return jo;
		} catch (Exception e) {
			LOGGER.error(e);
			throw new RuntimeException("Error rendering daily summary", e);
		}
	}

	@Override
	public String getClientWidgetType() {
		return "invcontrol";
	}

	@Override
	public void configure(Element root) throws IllegalArgumentException {
		super.configure(root);
		this.controllerName = InvUtil.getAttrib(root, "controller");

	}

	public String getControllerName() {
		return controllerName;
	}

}
