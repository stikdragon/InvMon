package uk.co.stikman.invmon.htmlout;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.datalog.QueryRecord;
import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.inverter.InvUtil;
import uk.co.stikman.log.StikLog;

public class HTMLOutput extends InvModule {
	private static final StikLog	LOGGER	= StikLog.getLogger(HTMLOutput.class);
	private File					target;
	private long					lastT;
	private DataLogger				datalogger;
	private static final String[]	COLOURS	= new String[] { "#ff7c7c", "#7cff7c", "#7c7cff", "#ff7cff" };

	public HTMLOutput(String id, Env env) {
		super(id, env);
	}

	@Override
	public void configure(Element config) {
		this.target = new File(InvUtil.getAttrib(config, "target"));
	}

	@Override
	public void start() throws InvMonException {
		super.start();
		this.datalogger = getEnv().getModule("datalogger");
	}

	@Subscribe(Events.POST_DATA)
	public void postData(PollData data) {
		long dt = System.currentTimeMillis() - lastT;
		if (dt > 5000) {
			try {
				lastT = System.currentTimeMillis();
				DataModel model = getEnv().getModel();
				//
				// render a nice page
				//
				HTMLBuilder html = new HTMLBuilder();
				html.append(getClass(), "top.html");

				//
				// data sets
				//
				int timeperiod = 1000 * 60 * 10;
				int pointCount = 120;

				//
				// to avoid ugly aliasing we'll snap to a point in future
				//
				long snap = timeperiod / 10;
				long cur = System.currentTimeMillis() / snap;
				cur++;
				cur *= snap;

				long limit = cur - timeperiod;
				List<Field> fields = new ArrayList<>();
				fields.add(model.get("TIMESTAMP"));
				fields.add(model.get("PV_TOTAL_P"));
				fields.add(model.get("PV1_P"));
				fields.add(model.get("PV2_P"));
				fields.add(model.get("PV3_P"));
				fields.add(model.get("PV4_P"));
				fields.add(model.get("LOAD_P"));
				fields.add(model.get("BATT_V"));
				fields.add(model.get("BATT_I"));
				fields.add(model.get("INV_MODE"));
				List<String> fieldsS = new ArrayList<>();
				fields.stream().map(Field::getId).forEach(fieldsS::add);
				QueryResults res = datalogger.query(limit, cur, pointCount, fieldsS);

				List<StringBuilder> arrays = new ArrayList<>();
				for (Field f : fields) {
					StringBuilder sb = new StringBuilder();
					sb.append("const d").append(f.getId()).append("=[");
					arrays.add(sb);
				}
				String sep = "";
				for (QueryRecord rec : res.getRecords()) {
					int i = 0;
					for (StringBuilder sb : arrays) {
						switch (fields.get(i).getDataType()) {
							case FLOAT:
								sb.append(sep).append(rec.getFloat(i));
								break;
							case STRING:
								sb.append(sep).append("\"").append(rec.getString(i)).append("\"");
								break;
						}
						++i;
					}
					sep = ",";
				}
				for (StringBuilder sb : arrays)
					sb.append("];\n");

				for (StringBuilder sb : arrays)
					html.append(sb.toString());

				html.append("</script></head><body>");

				html.div("sect").append("<h1>PV Power</h1>");
				html.append("<canvas id=\"c1\" width=\"500\" height=\"280\"></canvas>");
				html.append("</div>\n");

				html.div("sect").append("<h1>Load</h1>");
				html.append("<canvas id=\"c2\" width=\"500\" height=\"280\"></canvas>");
				html.append("</div>\n");

				html.div("sect").append("<h1>Battery Current</h1>");
				html.append("<canvas id=\"c3\" width=\"500\" height=\"280\"></canvas>");
				html.append("</div>\n");

				StringBuilder datasets = new StringBuilder();
				datasets.append("{").append(generateChartDSEntry("PV1_P", "hsl(30, 75%, 75%)")).append("},");
				datasets.append("{").append(generateChartDSEntry("PV2_P", "hsl(60, 75%, 75%)")).append("},");
				datasets.append("{").append(generateChartDSEntry("PV3_P", "hsl(90, 75%, 75%)")).append("},");
				datasets.append("{").append(generateChartDSEntry("PV4_P", "hsl(120, 75%, 75%)")).append("},");

				html.append("<script>\n");
				String s = HTMLBuilder.readResource(getClass(), "chart1.js");
				s = s.replaceAll("DATASETS", datasets.toString());
				s = s.replaceAll("CHARTNAME", "c1");
				html.append(s);
				html.append("</script>\n");

				html.append(getClass(), "bottom.html");

				try (FileOutputStream fos = new FileOutputStream(target)) {
					fos.write(html.toString().getBytes(StandardCharsets.UTF_8));
				}
			} catch (Exception e) {
				LOGGER.error(e);
			}
		}
	}

	private String generateChartDSEntry(String sourcefield, String colour) {
		StringBuilder sb = new StringBuilder();
		sb.append("fill: true,\n");
		sb.append("backgroundColor: \"").append(colour).append("\",\n");
		//		sb.append("pointBackgroundColor: colors.purple.stroke,\n");
		//		sb.append("borderColor: colors.purple.stroke,\n");
		//		sb.append("pointHighlightStroke: colors.purple.stroke,\n");
		//		sb.append("borderCapStyle: 'butt',\n");
		sb.append("data: d").append(sourcefield).append(",\n");
		sb.append("lineTension: 0,\n");
		sb.append("pointRadius: 0,\n");
		sb.append("borderWidth: 0,\n");

		return sb.toString();
	}

}
