package uk.co.stikman.invmon.server.widgets;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import uk.co.stikman.invmon.datalog.QueryRecord;
import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.minidom.MDElement;
import uk.co.stikman.invmon.server.HTMLBuilder;
import uk.co.stikman.invmon.server.PageLayout;
import uk.co.stikman.invmon.server.UserSesh;
import uk.co.stikman.invmon.shared.WidgetConfigOptions;
import uk.co.stikman.log.StikLog;

public class DailyPowerSummaryWidget extends PageWidget {

	private static final StikLog	LOGGER					= StikLog.getLogger(DailyPowerSummaryWidget.class);
	private static final String		DAILY_POWER_USAGE_SET	= "daily-power-qr";
	private static final long		REQUERY_INTERVAL		= 30000;											// in ms

	private static final float		MILLIS_PER_HOUR			= 1000 * 60 * 60;

	private List<String>			fieldNames				= new ArrayList<>();
	private List<String>			descriptions			= new ArrayList<>();

	private class OurData {
		QueryResults	data;
		long			timestamp;
		float			totalPV;
		float			totalLoad;
		float			battIn;
		float			battOut;
		float			gridIn;

		public OurData(QueryResults data) {
			super();
			this.data = data;
			timestamp = System.currentTimeMillis();
		}

	}

	public DailyPowerSummaryWidget(PageLayout owner) {
		super(owner);
	}

	@Override
	public JSONObject executeApi(UserSesh sesh, String api, JSONObject args) {
		if ("execute".equals(api)) {
			//
			// so we can't use the normal cached recordset because it's going to be for a differet
			// time range, so we'll manage our own here instead
			//
			try {
				OurData qr = sesh.getData(DAILY_POWER_USAGE_SET);
				if (qr != null)
					if (System.currentTimeMillis() - qr.timestamp > REQUERY_INTERVAL)
						qr = null;
				if (qr == null) {
					DataModel mdl = getOwner().getEnv().getModel();
					List<String> flds = new ArrayList<>();
					flds.add("PV_TOTAL_P");
					flds.add("LOAD_P");
					flds.add("GRID_I");
					flds.add("GRID_V");
					flds.add("BATT_I");
					flds.add("BATT_V");

					//
					// requery from previous midnight to now
					//
					ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
					ZonedDateTime midnight = now.toLocalDate().atStartOfDay(ZoneId.systemDefault());
					QueryResults data = getDatalogger().query(midnight.toEpochSecond() * 1000, now.toEpochSecond() * 1000, 1000, flds);
					qr = new OurData(data);
					sesh.putData(DAILY_POWER_USAGE_SET, qr);

					int fpv = data.getFieldIndex("PV_TOTAL_P");
					int fload = data.getFieldIndex("LOAD_P");
					int fgi = data.getFieldIndex("GRID_I");
					int fgv = data.getFieldIndex("GRID_V");
					int fbi = data.getFieldIndex("BATT_I");
					int fbv = data.getFieldIndex("BATT_V");

					float hours = (data.getEnd() - data.getStart()) / MILLIS_PER_HOUR;
					float slicewidth = data.getRecords().isEmpty() ? 0.0f : hours / data.getRecords().size();
					for (QueryRecord r : data.getRecords()) {
						qr.totalPV += r.getFloat(fpv) * slicewidth;
						qr.totalLoad += r.getFloat(fload) * slicewidth;
						float f = r.getFloat(fbi) * r.getFloat(fbv) * slicewidth;
						if (f < 0.0f)
							qr.battOut += -f;
						else
							qr.battIn += f;
						qr.gridIn += r.getFloat(fgv) * r.getFloat(fgi) * slicewidth;
					}
				}

				HTMLBuilder html = new HTMLBuilder();
				html.append("<p class=\"tiny\">Totals since midnight</p>");
				html.append("<table class=\"data\">");
				html.append("<tr><th>PV Total</th><td>").append(String.format("<td><span class=\"b\">%.2f</span>kWh</td>", qr.totalPV / 1000.0f)).append("</td></tr>");
				html.append("<tr><th>Load</th><td>").append(String.format("<td><span class=\"b\">%.2f</span>kWh</td>", qr.totalLoad / 1000.0f)).append("</td></tr>");
				html.append("<tr><th>Charged</th><td>").append(String.format("<td><span class=\"b\">%.2f</span>kWh</td>", qr.battIn / 1000.0f)).append("</td></tr>");
				html.append("<tr><th>Discharged</th><td>").append(String.format("<td><span class=\"b\">%.2f</span>kWh</td>", qr.battOut / 1000.0f)).append("</td></tr>");
				html.append("<tr><th>Grid</th><td>").append(String.format("<td><span class=\"b\">%.2f</span>kWh</td>", qr.gridIn / 1000.0f)).append("</td></tr>");
				html.append("</table>");
				JSONObject jo = new JSONObject();
				jo.put("html", html.toString());
				return jo;
			} catch (Exception e) {
				LOGGER.error(e);
				throw new RuntimeException("Error rendering daily summary", e);
			}
		}
		return super.executeApi(sesh, api, args);
	}

	@Override
	public String getClientWidgetType() {
		return "serverside";
	}

	@Override
	public WidgetConfigOptions getConfigOptions() {
		return new WidgetConfigOptions();
	}

	@Override
	public void applyConfigOptions(WidgetConfigOptions opts) {
	}

}
