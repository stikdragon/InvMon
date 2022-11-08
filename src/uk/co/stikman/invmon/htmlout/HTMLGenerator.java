package uk.co.stikman.invmon.htmlout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.datalog.QueryRecord;
import uk.co.stikman.invmon.datalog.QueryResults;

public class HTMLGenerator {
	private static final String[]	COLOURS			= new String[] { "#ff7c7c", "#7cff7c", "#7c7cff", "#ff7cff" };
	private DataLogger				source;
	private static final long[]		TIMESCALES		= new long[] { 5, 30, 60, 2 * 60, 12 * 60, 24 * 60, 5 * 24 * 60, 30 * 24 * 60 };
	private static final int[]		TIMESCALE_TYPE	= new int[] { 0, 0, 1, 1, 1, 1, 2, 2 };											// min, hour, day

	public HTMLGenerator(DataLogger datalogger) {
		this.source = datalogger;
	}

	public void render(HTMLBuilder html) {
		HTMLOpts def = new HTMLOpts();
		def.setDuration(5 * 60);
		render(html, def);
	}

	public void render(HTMLBuilder html, HTMLOpts opts) {
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

		html.append("<div>").div("sect").append("<h1>PV Power</h1>");
		renderPVPowerChart(html, opts);
		html.append("</div></div>");

		html.append("<div>").div("sect").append("<h1>Load</h1>");
		renderLoadChart(html, opts);
		html.append("</div></div>");

		html.append("<div>").div("sect").append("<h1>Battery Current</h1>");
		renderBatteryChart(html, opts);
		html.append("</div></div>");

		html.append("<div class=\"tiny\"><div class=\"a\">Render time: </div><div class=\"b\">").append(System.currentTimeMillis() - lastT).append("ms</div></div>");
		html.append("<div class=\"tiny\"><div class=\"a\">Local Time: </div><div class=\"b\">").append(new Date().toString()).append("</div></div>");
		html.append("<div class=\"tiny\"><div class=\"a\">Version: </div><div class=\"b\">").append(Env.VERSION).append("</div></div>");
		html.append(getClass(), "bottom_static.html");
	}

	private void renderPVPowerChart(HTMLBuilder html, HTMLOpts opts) {
		ChartOptions co = new ChartOptions(120, opts.getDuration() * 1000 * 60);
		co.addSeries("PV_TOTAL_P", list("PV1_P", "PV2_P", "PV3_P", "PV4_P"));
		co.getAxisY1().setFormatter(f -> String.format("%d W", f.intValue()));
		renderChart(html, co);
	}

	private void renderLoadChart(HTMLBuilder html, HTMLOpts opts) {
		ChartOptions co = new ChartOptions(120, opts.getDuration() * 1000 * 60);
		co.addSeries("LOAD_P").setFill("#ffc456");
		co.getAxisY1().setFormatter(f -> String.format("%d W", f.intValue()));
		renderChart(html, co);
	}

	private void renderBatteryChart(HTMLBuilder html, HTMLOpts opts) {
		ChartOptions co = new ChartOptions(120, opts.getDuration() * 1000 * 60);
		co.getAxisY1().setFormatter(f -> String.format("%.1f A", f.floatValue()));
		co.addSeries("BATT_V").setFill("#c4ff56");
		co.addSeries("BATT_I");
		renderChart(html, co);
	}

	private SimpleDateFormat sdf = new SimpleDateFormat("yyy/MM/dd HH:mm:ss");

	private void renderChart(HTMLBuilder html, ChartOptions opts) {
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
			html.append("<svg class=\"chart\" width=\"%dpx\" height=\"%dpx\">\n", width, height);
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
				sb.append("<path d=\"M0 ").append(height - 0).append(" ");
				f = 0.0f;
				for (QueryRecord rec : res.getRecords()) {
					long ts = rec.getLong(fts);
					f = rec.getFloat(fmain);
					sb.append("L").append(width * (float) (ts - tsStart) / tsLength).append(" ").append(height - (height * f / scaleP)).append(" ");
				}
				sb.append("L").append(width).append(" ").append(height - 0).append(" ");
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
					sb.append("<path d=\"M0 ").append(height - 0).append(" ");
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
					sb.append("\" stroke=\"none\" fill=\"" + colours[i] + "\"/>\n");
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
