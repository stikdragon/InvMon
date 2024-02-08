package uk.co.stikman.invmon.server.widgets;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.json.JSONObject;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.server.HTMLBuilder;
import uk.co.stikman.invmon.server.PageLayout;
import uk.co.stikman.invmon.server.UserSesh;
import uk.co.stikman.invmon.shared.WidgetConfigOptions;

public class InfoBitWidget extends PageWidget {

	public InfoBitWidget(PageLayout owner) {
		super(owner);
	}

	@Override
	public JSONObject executeApi(UserSesh sesh, String api, JSONObject args) {
		if ("execute".equals(api)) {
			HTMLBuilder html = new HTMLBuilder();
			html.append("<div class=\"tiny\"><div class=\"a\">Local Time: </div><div class=\"b\">").append(new Date().toString()).append("</div></div>");
			html.append("<div class=\"tiny\"><div class=\"a\">Version: </div><div class=\"b\">").append(Env.getVersion()).append("</div></div>");

			long n = ManagementFactory.getRuntimeMXBean().getUptime();
			Duration d = Duration.of(n, ChronoUnit.MILLIS);
			String dur = String.format("%d days, %02d:%02d", d.toDays() % 60, d.toHours() % 24, d.toMinutes() % 60);
			html.append("<div class=\"tiny\"><div class=\"a\">Uptime: </div><div class=\"b\">").append(dur).append("</div></div>");

			n = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
			html.append("<div class=\"tiny\"><div class=\"a\">Heap/Max: </div><div class=\"b\">").append(InvUtil.formatSize(n)).append(" / ").append(InvUtil.formatSize(Runtime.getRuntime().maxMemory())).append("</div></div>");

			DataLogger dl = getDatalogger();
			if (dl != null) {
				html.append("<div class=\"tiny\"><div class=\"a\">DB Size: </div><div class=\"b\">").append(InvUtil.formatSize(dl.getDbFileSize())).append("</div></div>");
				html.append("<div class=\"tiny\"><div class=\"a\">Sample Count: </div><div class=\"b\">").append(String.format("%,d", dl.getDbRecordCount())).append("</div></div>");
			}

			JSONObject jo = new JSONObject();
			jo.put("html", html.toString());
			return jo;
		}

		return super.executeApi(sesh, api, args);
	}

	@Override
	public String getClientWidgetType() {
		return "infobit";
	}

	@Override
	public WidgetConfigOptions getConfigOptions() {
		return new WidgetConfigOptions();
	}

	@Override
	public void applyConfigOptions(WidgetConfigOptions opts) {
	}

}
