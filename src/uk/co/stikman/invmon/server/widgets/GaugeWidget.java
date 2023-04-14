package uk.co.stikman.invmon.server.widgets;

import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.server.HTMLBuilder;
import uk.co.stikman.invmon.server.HTTPServer;
import uk.co.stikman.invmon.server.UserSesh;
import uk.co.stikman.invmon.server.WidgetExecuteContext;
import uk.co.stikman.log.StikLog;

public class GaugeWidget extends PageWidget {
	private static final StikLog LOGGER = StikLog.getLogger(GaugeWidget.class);

	@Override
	public JSONObject execute(JSONObject params, WidgetExecuteContext ctx) {
		try {
			UserSesh sesh = ctx.getSession();
			DBRecord rec = sesh.getData(HTTPServer.CACHED_LAST_RECORD);
			if (rec == null)
				throw new RuntimeException("No data");

			HTMLBuilder html = new HTMLBuilder();
			html.append("<div>");
			html.append("<svg viewBox=\"0 0 100 50\">");
			html.append("<g transform=\"rotate(-90 50 50)\" fill=\"#ff2222\">");
			html.append("<path d=\"M 0 50 A 50 50 0 0 1 100 50 L 80 50 A 30 30 0 0 0 20 50\" />");
			html.append("</g>");
			html.append("<g transform=\"rotate(90 50 50)\" fill=\"#FFBF00\">");
			html.append("<path d=\"M 0 50 A 50 50 0 0 1 100 50 L 80 50 A 30 30 0 0 0 20 50\" />");
			html.append("</g>");
			html.append("<g transform=\"rotate(120 50 50)\" fill=\"#22FF22\">");
			html.append("<path d=\"M 0 50 A 50 50 0 0 1 100 50 L 80 50 A 30 30 0 0 0 20 50\" />");
			html.append("</g>");

			float soc = rec.getFloat(ctx.getOwner().getEnv().getModel().get("BATT_SOC")); // 0..1
			float rot = soc * 180.0f - 90.0f;

			html.append("<g transform=\"rotate(" + rot + " 50 50)\" fill=\"000000\">");
			html.append("<path d=\"M 45 50 L 50 10 L 55 50\" />");
			html.append("</g>");

			html.append("<circle fill=\"000000\" cx=\"50\" cy=\"50\" r=\"10\" />");
			html.append("</svg>");
			html.append("<div class=\"reading\">");
			html.append((int) (soc * 100.0f) + "%");
			html.append("</div>");
			html.append("</div>");

			JSONObject jo = new JSONObject();
			jo.put("html", html.toString());
			return jo;
		} catch (Exception e) {
			LOGGER.error(e);
			throw new RuntimeException("Error rendering daily summary", e);
		}
	}

	@Override
	public String getClientWidgetType() {
		return "serverside";
	}

	@Override
	public void configure(Element root) {
		super.configure(root);
	}

}
