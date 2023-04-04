package uk.co.stikman.invmon.htmlout;

import java.util.Date;

import org.json.JSONObject;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.inverter.util.InvUtil;

public class InfoBitWidget extends PageWidget {

	public InfoBitWidget() {
		super();
	}

	@Override
	public JSONObject execute(JSONObject params, WidgetExecuteContext qr) {
		HTMLBuilder html = new HTMLBuilder();
		html.append("<div class=\"tiny\"><div class=\"a\">Local Time: </div><div class=\"b\">").append(new Date().toString()).append("</div></div>");
		html.append("<div class=\"tiny\"><div class=\"a\">Version: </div><div class=\"b\">").append(Env.getVersion()).append("</div></div>");

		DataLogger dl = null;
		for (InvModule m : qr.getOwner().getEnv().getModules()) {
			if (m instanceof DataLogger)
				dl = (DataLogger) m;
		}
		if (dl != null) {
			html.append("<div class=\"tiny\"><div class=\"a\">DB Size: </div><div class=\"b\">").append(InvUtil.formatSize(dl.getDbFileSize())).append("</div></div>");
			html.append("<div class=\"tiny\"><div class=\"a\">Sample Count: </div><div class=\"b\">").append(String.format("%,d", dl.getDbRecordCount())).append("</div></div>");
		}

		JSONObject jo = new JSONObject();
		jo.put("html", html.toString());
		return jo;
	}

	@Override
	public String getClientWidgetType() {
		return "infobit";
	}

}
