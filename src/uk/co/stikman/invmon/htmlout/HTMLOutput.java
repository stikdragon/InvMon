package uk.co.stikman.invmon.htmlout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.HTMLConsoleThing;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.TempHTMLOutput;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.datalog.QueryRecord;
import uk.co.stikman.invmon.datalog.QueryResults;
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
			lastT = System.currentTimeMillis();

			//
			// render a nice page
			//
			HTMLBuilder html = new HTMLBuilder();
			html.append(getClass(), "top.html");

			html.div("sect").append("<h1>PV Power</h1>");
			renderPVPowerChart(html);
			html.append("</div>");

			html.div("sect").append("<h1>Load</h1>");
			renderLoadChart(html);
			html.append("</div>");

			html.div("sect").append("<h1>Battery Current</h1>");
			renderBatteryChart(html);
			html.append("</div>");

			html.append(getClass(), "bottom.html");

			try (FileOutputStream fos = new FileOutputStream(target)) {
				fos.write(html.toString().getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}

	private void renderPVPowerChart(HTMLBuilder html) {
		ChartOptions opts = new ChartOptions(120, 5 * 60 * 1000);
		opts.addSeries("PV_TOTAL_P", list("PV1_P", "PV2_P", "PV3_P", "PV4_P"));
		renderChart(html, opts);
	}

	private void renderLoadChart(HTMLBuilder html) {
		ChartOptions opts = new ChartOptions(120, 5 * 60 * 1000);
		opts.addSeries("LOAD_P").setFill("#ffc456");
		renderChart(html, opts);
	}

	private void renderBatteryChart(HTMLBuilder html) {
		ChartOptions opts = new ChartOptions(120, 5 * 60 * 1000);
		opts.addSeries("BATT_V").setFill("#c4ff56");
		opts.addSeries("BATT_I");
		renderChart(html, opts);
	}

	private void renderChart(HTMLBuilder html, ChartOptions opts) {
		//
		// this is quite messy i'm afraid
		//
		int timeperiod = opts.getTimePeriod();

		//
		// to avoid ugly aliasing we'll snap to a point in future
		//
		long snap = timeperiod / 10;
		long cur = System.currentTimeMillis() / snap;
		cur++;
		cur *= snap;

		long limit = cur - timeperiod;
		List<String> fields = new ArrayList<>();
		for (Series s : opts.getSeries()) {
			fields.add(s.getField());
			fields.addAll(s.getSubfields());
		}
		QueryResults res = datalogger.query(limit, cur, opts.getPointCount(), fields);

		//
		// now onto rendering the chart
		//
		final int w = 400;
		final int h = 200;
		html.append("<svg width=\"" + w + "px\" height=\"" + h + "px\">\n");
		int fts = res.getFieldIndex("TIMESTAMP");

		for (Series series : opts.getSeries()) {
			int fmain = res.getFieldIndex(series.getField());
			int[] fsubs = new int[series.getSubfields().size()];
			String[] colours = new String[fsubs.length];
			for (int i = 0; i < fsubs.length; ++i) {
				fsubs[i] = res.getFieldIndex(series.getSubfields().get(i));
				colours[i] = COLOURS[i % COLOURS.length];
			}

			int rcount = res.getRecords().size();
			float[] points = new float[2 * (rcount + 2)]; // 2 extra points for the corners
			int p = 0;
			points[p++] = 0.0f;
			points[p++] = h;

			float scaleP = 1.0f;
			for (QueryRecord rec : res.getRecords()) {
				float f = rec.getFloat(fmain);
				if (f > scaleP)
					scaleP = f;
			}

			//
			// align to a major increments
			//
			int oom = scaleP == 0.0f ? 0 : (int) Math.log10(scaleP);
			float f = (float) Math.pow(10, oom - 1);
			scaleP = (float) (f * Math.floor((scaleP) / f)) * 1.1f;

			List<String> pathlist = new ArrayList<>();
			//
			// total line
			//
			StringBuilder sb = new StringBuilder();
			sb.append("<path d=\"M0 ").append(h - 0).append(" ");
			f = 0.0f;
			for (QueryRecord rec : res.getRecords()) {
				long ts = rec.getLong(fts);
				f = rec.getFloat(fmain);
				sb.append("L").append(w * (float) (ts - limit) / timeperiod).append(" ").append(h - (h * f / scaleP)).append(" ");
			}
			sb.append("L").append(w).append(" ").append(h - 0).append(" ");
			sb.append("\" stroke=\"#000000a0\" fill=\"" + series.getFill() + "\" stroke-width=\"2px\" />\n");
			pathlist.add(sb.toString());

			//
			// polygons for each sub-measure, if we have them
			//
			float[] offsets = new float[rcount];
			for (int i = 0; i < offsets.length; ++i)
				offsets[i] = 0.0f;
			for (int i = 0; i < fsubs.length; ++i) {
				sb = new StringBuilder();
				sb.append("<path d=\"M0 ").append(h - 0).append(" ");
				int j = 0;
				for (QueryRecord rec : res.getRecords()) {
					long ts = rec.getLong(fts);
					f = rec.getFloat(fsubs[i]);
					float y = h * f / scaleP;
					sb.append("L").append(w * (float) (ts - limit) / timeperiod).append(" ").append(h - (y + offsets[j])).append(" ");
					offsets[j] += y;
					++j;
				}

				sb.append("L").append(w).append(" ").append(h - 0).append(" ");
				sb.append("\" stroke=\"none\" fill=\"" + colours[i] + "\"/>\n");
				pathlist.add(sb.toString());
			}

			for (int i = pathlist.size() - 1; i >= 0; --i)
				html.append(pathlist.get(i));

		}

		html.append("</svg>\n");
	}

	private static List<String> list(String... strings) {
		List<String> a = new ArrayList<>();
		for (String s : strings)
			a.add(s);
		return a;
	}

}
