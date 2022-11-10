package uk.co.stikman.invmon.htmlout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import uk.co.stikman.invmon.DataPoint;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.datalog.QueryRecord;
import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.datamodel.VIFReading;

public class HTMLGenerator {
	private static final String[]	COLOURS			= new String[] { "#ff7c7c", "#7cff7c", "#7c7cff", "#ff7cff" };
	private DataLogger				source;
	private static final long[]		TIMESCALES		= new long[] { 5, 30, 60, 2 * 60, 12 * 60, 24 * 60, 5 * 24 * 60, 30 * 24 * 60 };
	private static final int[]		TIMESCALE_TYPE	= new int[] { 0, 0, 1, 1, 1, 1, 2, 2 };											// min, hour, day

	public HTMLGenerator(DataLogger datalogger) {
		this.source = datalogger;
	}

	public void render(HTMLBuilder html, PollData data) {
		HTMLOpts def = new HTMLOpts();
		def.setDuration(5 * 60);
		render(html, def, data);
	}

	public void render(HTMLBuilder html, HTMLOpts opts, PollData data) {
		//
		// render a nice page
		//
		long lastT = System.currentTimeMillis();
		html.append(getClass(), "top_static.html");

		html.div("sect");
		html.div("controls");
		int i = 0;
		for (long n : TIMESCALES) {
			String s = null;
			switch (TIMESCALE_TYPE[i++]) {
				case 0:
					s = n + " Min";
					break;
				case 1:
					s = (n / 60) + " Hour";
					break;
				case 2:
					s = (n / 1440) + " Day";
					break;
			}
			if (opts.getDuration() == n)
				html.div("sel").append(s).append("</div>");
			else
				html.div("unsel").append("<a href=\"?dur=" + n + "\">" + s + "</a></div>");
		}
		html.append("</div></div>");

		DataPoint dp = data.get("invA");

		VIFReading vif1 = dp.get(source.getEnv().getModel().getVIF("PV1"));
		VIFReading vif2 = dp.get(source.getEnv().getModel().getVIF("PV2"));

		html.append("<div>").div("sect").append("<div class=\"hdr\"><h1>PV Power</h1>");
		renderVIF(html, "PV1", vif1);
		renderVIF(html, "PV2", vif2).append("</div>");
		renderPVPowerChart(html, opts);
		html.append("</div></div>");

		html.append("<div>").div("sect").append("<div class=\"hdr\"><h1>Load</h1>");
		vif1 = dp.get(source.getEnv().getModel().getVIF("LOAD"));
		renderVIF(html, "Load", vif1).append("</div>");
		renderLoadChart(html, opts);
		html.append("</div></div>");

		html.append("<div>").div("sect").append("<div class=\"hdr\"><h1>Battery Current</h1>");
		vif1 = dp.get(source.getEnv().getModel().getVIF("BATT"));
		renderVIF(html, "Batt", vif1).append("</div>");
		renderBatteryChart(html, opts);
		html.append("</div></div>");

		html.append("<div class=\"tiny\"><div class=\"a\">Render time: </div><div class=\"b\">").append(System.currentTimeMillis() - lastT).append("ms</div></div>");
		html.append("<div class=\"tiny\"><div class=\"a\">Local Time: </div><div class=\"b\">").append(new Date().toString()).append("</div></div>");
		html.append("<div class=\"tiny\"><div class=\"a\">Version: </div><div class=\"b\">").append(Env.VERSION).append("</div></div>");
		html.append(getClass(), "bottom_static.html");
	}

	private HTMLBuilder renderVIF(HTMLBuilder html, String name, VIFReading vif) {
		html.append("<div class=\"grp\">");
		html.append("<span class=\"a\">%s: </span><span class=\"b\">%d</span><span class=\"a\">W (</span><span class=\"b\">%.1f</span><span class=\"a\">V @ </span><span class=\"b\">%.2f</span><span class=\"a\">A)</span>", name, (int) vif.getP(), vif.getV(), vif.getI());
		html.append("</div>");
		return html;
	}

	private void renderPVPowerChart(HTMLBuilder html, HTMLOpts opts) {
		ChartOptions co = new ChartOptions(120, opts.getDuration() * 1000 * 60);
		co.addSeries("PV_TOTAL_P", list("PV1_P", "PV2_P", "PV3_P", "PV4_P"));
		co.getAxisY1().setFormatter(f -> String.format("%d W", f.intValue()));
		renderChart(html, "pv", co);
	}

	private void renderLoadChart(HTMLBuilder html, HTMLOpts opts) {
		ChartOptions co = new ChartOptions(120, opts.getDuration() * 1000 * 60);
		co.addSeries("LOAD_P");
		co.getAxisY1().setFormatter(f -> String.format("%d W", f.intValue()));
		renderChart(html, "load", co);
	}

	private void renderBatteryChart(HTMLBuilder html, HTMLOpts opts) {
		ChartOptions co = new ChartOptions(120, opts.getDuration() * 1000 * 60);
		co.getAxisY1().setFormatter(f -> String.format("%.1f A", f.floatValue()));
		co.addSeries("BATT_I");
		co.addSeries("BATT_V");
		renderChart(html, "battery", co);
	}

	private SimpleDateFormat sdf = new SimpleDateFormat("yyy/MM/dd HH:mm:ss");

	private void renderChart(HTMLBuilder html, String cssclass, ChartOptions opts) {
		try {
			//
			// this is quite messy i'm afraid
			//
			opts.getAxisX1().setFormatter(f -> sdf.format(new Date(f.longValue())));

			//
			// so snap to nearest N millis, depending on how big our range is, to 
			// avoid aliasing
			//
			int N = 1;
			long tsStart = opts.getStartTime();
			long tsEnd = opts.getEndTime();
			long tsLength = tsEnd - tsStart;
			if (tsLength <= 60000)
				N = 2500;
			else if (tsLength <= 60000 * 10)
				N = 30000;
			else
				N = 30000 * 5;

			tsEnd = tsEnd / N;
			++tsEnd;
			tsEnd *= N;

			tsLength = tsLength / N;
			++tsLength;
			tsLength *= N;

			tsStart = tsEnd - tsLength;

			List<String> fields = new ArrayList<>();
			for (Series s : opts.getSeries()) {
				fields.add(s.getField());
				fields.addAll(s.getSubfields());
			}

			QueryResults res = source.query(tsStart, tsEnd, opts.getPointCount(), fields);

			//
			// now onto rendering the chart
			//
			final int AXIS_W = 55;
			final int AXIS_H = 22;
			final int width = 800;
			final int height = 300;
			int nAxes = opts.getAxisY2() != null ? 2 : 1;
			float sx = 1.0f - (float) (AXIS_W * nAxes) / width; // scale factors
			float sy = 1.0f - (float) AXIS_H / height;
			html.append("<svg class=\"chart ").append(cssclass).append("\" width=\"%dpx\" height=\"%dpx\">\n", width, height);
			html.append("<g transform=\"translate(%d,0) scale(%f, %f)\"> \n", AXIS_W, sx, sy);
			html.append("<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" class=\"background\" />", 0, 0, width, height);
			int fts = res.getFieldIndex("TIMESTAMP");

			long minX = Long.MAX_VALUE;
			long maxX = Long.MIN_VALUE;
			for (QueryRecord rec : res.getRecords()) {
				minX = Math.min(minX, rec.getLong(fts));
				maxX = Math.max(maxX, rec.getLong(fts));
			}

			float ax1Min = Float.MAX_VALUE;
			float ax1Max = Float.MIN_VALUE;
			int seriesIndex = 0;
			for (Series series : opts.getSeries()) {
				++seriesIndex;
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
				points[p++] = height;

				float scaleP = 1.0f;
				for (QueryRecord rec : res.getRecords()) {
					float f = rec.getFloat(fmain);
					if (f > scaleP)
						scaleP = f;
					ax1Min = Math.min(ax1Min, f);
					ax1Max = Math.max(ax1Max, f);
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
				sb.append("<path class=\"series" + seriesIndex + "\" d=\"M0 ").append(height - 0).append(" ");
				f = 0.0f;
				for (QueryRecord rec : res.getRecords()) {
					long ts = rec.getLong(fts);
					f = rec.getFloat(fmain);
					sb.append("L").append(width * (float) (ts - tsStart) / tsLength).append(" ").append(height - (height * f / scaleP)).append(" ");
				}
				sb.append("L").append(width).append(" ").append(height - 0).append(" ");
				sb.append("\" />\n");
				pathlist.add(sb.toString());

				//
				// polygons for each sub-measure, if we have them
				//
				float[] offsets = new float[rcount];
				for (int i = 0; i < offsets.length; ++i)
					offsets[i] = 0.0f;
				for (int i = 0; i < fsubs.length; ++i) {
					sb = new StringBuilder();
					sb.append("<path class=\"series" + seriesIndex + "_" + (i + 1) + "\" d=\"M0 ").append(height - 0).append(" ");
					int j = 0;
					for (QueryRecord rec : res.getRecords()) {
						long ts = rec.getLong(fts);
						f = rec.getFloat(fsubs[i]);
						float y = height * f / scaleP;
						sb.append("L").append(width * (float) (ts - tsStart) / tsLength).append(" ").append(height - (y + offsets[j])).append(" ");
						offsets[j] += y;
						++j;
					}

					sb.append("L").append(width).append(" ").append(height - 0).append(" ");
					sb.append("\"/>\n");
					pathlist.add(sb.toString());
				}

				for (int i = pathlist.size() - 1; i >= 0; --i)
					html.append(pathlist.get(i));

			}
			html.append("</g>\n");

			//
			// do axes
			//
			Function<Float, String> f = opts.getAxisY1().getFormatter();
			html.append("<path d=\"M%d %d %d %d\" stroke-width=\"2\" stroke=\"black\"/>\n", AXIS_W, 0, AXIS_W, height - AXIS_H);
			html.append("<text x=\"%d\" y=\"%d\" alignment-baseline=\"hanging\" text-anchor=\"end\" class=\"axis\">%s</text>", AXIS_W - 4, 0, f.apply(ax1Max));
			html.append("<text x=\"%d\" y=\"%d\" text-anchor=\"end\" class=\"axis\">%s</text>", AXIS_W - 4, height - AXIS_H, f.apply(ax1Min));

			Function<Long, String> f2 = opts.getAxisX1().getFormatter();
			html.append("<path d=\"M%d %d %d %d\" stroke-width=\"2\" stroke=\"black\"/>\n", AXIS_W, height - AXIS_H, width, height - AXIS_H);
			html.append("<text x=\"%d\" y=\"%d\" alignment-baseline=\"hanging\" class=\"axis\">%s</text>", AXIS_W, height - AXIS_H + 4, f2.apply(minX));
			html.append("<text x=\"%d\" y=\"%d\" alignment-baseline=\"hanging\" text-anchor=\"end\" class=\"axis\">%s</text>", width, height - AXIS_H + 4, f2.apply(maxX));

			html.append("<circle cx=\"%d\" cy=\"%d\" r=\"2\" fill=\"black\" />", AXIS_W, height - AXIS_H);
			html.append("<circle cx=\"%d\" cy=\"%d\" r=\"2\" fill=\"black\" />", width, height - AXIS_H);
			html.append("<circle cx=\"%d\" cy=\"%d\" r=\"2\" fill=\"black\" />", AXIS_W, 0);

			html.append("</svg>\n");
		} catch (Exception e) {
			html.clear();
			html.append("Error: " + e.getMessage());
		}
	}

	private static List<String> list(String... strings) {
		List<String> a = new ArrayList<>();
		for (String s : strings)
			a.add(s);
		return a;
	}
}
