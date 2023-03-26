package uk.co.stikman.invmon.htmlout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.datalog.MiniDbException;
import uk.co.stikman.invmon.datalog.QueryRecord;
import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.datamodel.VIFReading;
import uk.co.stikman.invmon.inverter.Tok;
import uk.co.stikman.invmon.inverter.TokenThing;
import uk.co.stikman.log.StikLog;

public class HTMLGenerator {
	private static final StikLog	LOGGER			= StikLog.getLogger(HTMLGenerator.class);
	private DataLogger				source;
	private static final long[]		TIMESCALES		= new long[] { 5, 30, 60, 2 * 60, 12 * 60, 24 * 60, 5 * 24 * 60, 30 * 24 * 60 };
	private static final int[]		TIMESCALE_TYPE	= new int[] { 0, 0, 1, 1, 1, 1, 2, 2 };											// min, hour, day

	public HTMLGenerator(DataLogger datalogger) {
		this.source = datalogger;
	}

	public void render(HTMLBuilder html) throws MiniDbException {
		ChartRenderConfig def = new ChartRenderConfig();
		def.setDuration(5 * 60);

		render(html, def);
	}

	public void render(HTMLBuilder html, ChartRenderConfig opts) throws MiniDbException {
		long end = System.currentTimeMillis();
		long start = end - opts.getDuration() * 1000 * 60;
		List<String> flds = new ArrayList<>();
		add(flds, "BATT_V", "BATT_I", "BATT_I_CHG", "BATT_I_DIS");
		add(flds, "LOAD_V", "LOAD_I", "LOAD_F");
		add(flds, "LOAD_P", "LOAD_1_P", "LOAD_2_P");
		add(flds, "PVA_1_V", "PVA_1_I", "PVA_1_P");
		add(flds, "PVB_1_V", "PVB_1_I", "PVB_1_P");
		add(flds, "PVA_2_V", "PVA_2_I", "PVA_2_P");
		add(flds, "PVB_2_V", "PVB_2_I", "PVB_2_P");
		add(flds, "PV_TOTAL_P");
		add(flds, "INV_1_TEMP", "INV_2_TEMP");
		add(flds, "INV_1_BUS_V", "INV_2_BUS_V");
		add(flds, "LOAD_PF");
		QueryResults qr = getQueryResults(start, end, flds, 120);

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

		//		System.out.println(qr.toString());
		//		DataPoint dp = data.get(sourceDataRef);

		DBRecord dp = source.getLastRecord();

		VIFReading vif1 = dp.getVIF(source.getEnv().getModel().getVIF("PVA_1"));
		VIFReading vif2 = dp.getVIF(source.getEnv().getModel().getVIF("PVB_1"));
		VIFReading vif3 = dp.getVIF(source.getEnv().getModel().getVIF("PVA_2"));
		VIFReading vif4 = dp.getVIF(source.getEnv().getModel().getVIF("PVB_2"));

		html.append("<div>").div("sect").append("<div class=\"hdr\"><h1>PV Power</h1>");
		renderGrp(html, "<div class=\"grp\">PV1: [%d]W ([%.2f]V @ [%.2f]A)</div>", (int) vif1.getP(), vif1.getV(), vif1.getI());
		renderGrp(html, "<div class=\"grp\">PV2: [%d]W ([%.2f]V @ [%.2f]A)</div>", (int) vif2.getP(), vif2.getV(), vif2.getI());
		renderGrp(html, "<div class=\"grp\">PV3: [%d]W ([%.2f]V @ [%.2f]A)</div>", (int) vif3.getP(), vif3.getV(), vif3.getI());
		renderGrp(html, "<div class=\"grp\">PV4: [%d]W ([%.2f]V @ [%.2f]A)</div>", (int) vif4.getP(), vif4.getV(), vif4.getI());
		renderGrp(html, "<div class=\"grp\">Total: [%d]W</div>", (int) (vif1.getP() + vif2.getP() + vif3.getP() + vif4.getP()));
		html.append("</div>");
		renderPVPowerChart(html, new ChartRenderConfig(), qr);
		html.append("</div></div>");

		html.append("<div>").div("sect").append("<div class=\"hdr\"><h1>Load</h1>");
		vif1 = dp.getVIF(source.getEnv().getModel().getVIF("LOAD"));
		renderGrp(html, "<div class=\"grp\">Load: [%d]W ([%.2f]V @ [%.2f]A)</div>", (int) vif1.getP(), vif1.getV(), vif1.getI());
		float pf = dp.getFloat(source.getEnv().getModel().get("LOAD_PF"));
		renderGrp(html, "<div class=\"grp\">PF: [%.2f] (Real Power: [%d]W @ [%.2f]A)</div>", pf, (int) (vif1.getP() * pf), vif1.getI() * pf);
		html.append("</div>");
		renderLoadChart(html, new ChartRenderConfig(), qr);
		html.append("</div></div>");

		html.append("<div>").div("sect").append("<div class=\"hdr\"><h1>Battery Current</h1>");
		vif1 = dp.getVIF(source.getEnv().getModel().getVIF("BATT"));
		renderVIF(html, "Batt", vif1).append("</div>");
		renderBatteryChart(html, new ChartRenderConfig(), qr);
		html.append("</div></div>");

		html.append("<div>").div("sect").append("<div class=\"hdr\"><h1>Temperatures/Bus</h1>");
		float ftmp1 = dp.getFloat(source.getEnv().getModel().get("INV_1_TEMP"));
		float ftmp2 = dp.getFloat(source.getEnv().getModel().get("INV_2_TEMP"));
		float busv1 = dp.getFloat(source.getEnv().getModel().get("INV_1_BUS_V"));
		float busv2 = dp.getFloat(source.getEnv().getModel().get("INV_2_BUS_V"));
		renderGrp(html, "<div class=\"grp\">Temp1: [%.1f]C  BusV: [%d]V</div>", ftmp1, (int) busv1);
		renderGrp(html, "<div class=\"grp\">Temp2: [%.1f]C  BusV: [%d]V</div>", ftmp2, (int) busv2);
		html.append("</div>");
		renderTempChart(html, new ChartRenderConfig(), qr);
		html.append("</div></div>");

		html.append("<div class=\"tiny\"><div class=\"a\">Render time: </div><div class=\"b\">").append(System.currentTimeMillis() - lastT).append("ms</div></div>");
		html.append("<div class=\"tiny\"><div class=\"a\">Local Time: </div><div class=\"b\">").append(new Date().toString()).append("</div></div>");
		html.append("<div class=\"tiny\"><div class=\"a\">Version: </div><div class=\"b\">").append(Env.getVersion()).append("</div></div>");
		html.append(getClass(), "bottom_static.html");
	}

	private static void add(List<String> lst, String... s) {
		for (String t : s)
			lst.add(t);
	}

	public QueryResults getQueryResults(long start, long end, List<String> fields, int pointcount) throws MiniDbException {
		//
		// so snap to nearest N millis, depending on how big our range is, to 
		// avoid aliasing
		//
		int N = 1;
		long tsStart = start;
		long tsEnd = end;
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

		return source.query(tsStart, tsEnd, pointcount, fields);

	}

	public HTMLBuilder renderVIF(HTMLBuilder html, String name, VIFReading vif) {
		html.append("<span class=\"a\">%s: </span><span class=\"b\">%d</span><span class=\"a\">W (</span><span class=\"b\">%.1f</span><span class=\"a\">V @ </span><span class=\"b\">%.2f</span><span class=\"a\">A)</span>", name, (int) vif.getP(), vif.getV(), vif.getI());
		return html;
	}

	public HTMLBuilder renderGrp(HTMLBuilder html, String fmt, Object... values) {
		TokenThing tt = new TokenThing(fmt);
		int p = 0;
		for (Tok t : tt) {
			if (t.isSquare()) {
				html.append("<span class=\"b\">").append(String.format(t.getText(), values[p++])).append("</span>");
			} else {
				html.append(t.getText());
			}
		}
		return html;
	}

	public void renderPVPowerChart(HTMLBuilder html, ChartRenderConfig opts, QueryResults data) {
		ChartOptions co = new ChartOptions();
		co.setSize(opts.getWidth(), opts.getHeight());
		co.addSeries("PV_TOTAL_P", list("PVA_1_P", "PVB_1_P", "PVA_2_P", "PVB_2_P"));
		co.getAxisY1().setForceMin(Float.valueOf(0));
		co.getAxisY1().setFormatter(f -> String.format("%d W", f.intValue()));
		renderChart(html, "pv", co, data);
	}

	public void renderLoadChart(HTMLBuilder html, ChartRenderConfig opts, QueryResults data) {
		ChartOptions co = new ChartOptions();
		co.setSize(opts.getWidth(), opts.getHeight());
		co.addSeries("LOAD_P", list("LOAD_1_P", "LOAD_2_P"));
		co.getAxisY1().setForceMin(Float.valueOf(0));
		co.getAxisY1().setFormatter(f -> String.format("%d W", f.intValue()));
		renderChart(html, "load", co, data);
	}

	public void renderBatteryChart(HTMLBuilder html, ChartRenderConfig opts, QueryResults data) {
		ChartOptions co = new ChartOptions();
		co.setSize(opts.getWidth(), opts.getHeight());
		co.getAxisY1().setFormatter(f -> String.format("%.1f A", f.floatValue()));
		co.getAxisY1().setForceMin(Float.valueOf(0));
		co.getAxisY2().setFormatter(f -> String.format("%.1f V", f.floatValue()));
		co.getAxisY2().setEnabled(true);
		co.getAxisY2().forceRange(35.0f, 65.0f);
		co.addSeries("BATT_I_CHG");
		co.addSeries("BATT_I_DIS");
		co.addSeries("BATT_V").setYAxis(co.getAxisY2());
		renderChart(html, "battery", co, data);
	}

	public void renderTempChart(HTMLBuilder html, ChartRenderConfig opts, QueryResults data) {
		ChartOptions co = new ChartOptions();
		co.setSize(opts.getWidth(), opts.getHeight());
		co.addSeries("INV_1_TEMP");
		co.addSeries("INV_2_TEMP");
		co.addSeries("INV_1_BUS_V").setYAxis(co.getAxisY2());
		co.getAxisY1().setFormatter(f -> String.format("%.1f C", f.floatValue()));
		co.getAxisY1().setForceMin(Float.valueOf(0));
		co.getAxisY2().setFormatter(f -> String.format("%d V", (int) f.floatValue()));
		co.getAxisY2().setEnabled(true);
		co.getAxisY2().setForceMin(Float.valueOf(0));
		renderChart(html, "temperature", co, data);
	}

	private static final String[] IDS = ",PVA_1,PVB_1,PVA_2,PVB_2".split(",");

	public static void renderPVTable(HTMLBuilder html, ChartRenderConfig opts, QueryResults data) {
		QueryRecord rec = data.getRecords().get(data.getRecords().size() - 1);
		html.append("<table class=\"data\">");
		html.append("<tr><td></td><th>P</th><th>V</th><th>I</th></tr>");
		for (int i = 1; i <= 4; ++i) {
			html.append("<tr>");
			html.append("<th>").append(IDS[i]).append("</th>");
			html.append("<td><span class=\"b\">").append((int) rec.getFloat(IDS[i] + "_P")).append("</span>W</td>");
			html.append("<td><span class=\"b\">").append((int) rec.getFloat(IDS[i] + "_V")).append("</span>V</td>");
			html.append(String.format("<td><span class=\"b\">%.1f</span>A</td>", rec.getFloat(IDS[i] + "_I")));
			html.append("</tr>");
		}

		html.append("<tr class=\"total\"><th>Total</th><td colspan=\"3\"><span class=\"b\">").append((int) rec.getFloat("PV_TOTAL_P")).append("</span>W</td></tr>");
		html.append("</table>");
	}

	/**
	 * "to-int"
	 * 
	 * @param f
	 * @return
	 */
	private static final String ti(float f) {
		return Integer.toString((int) f);
	}

	private static SimpleDateFormat sdf = new SimpleDateFormat("yy/MM/dd HH:mm");

	public static void renderChart(HTMLBuilder html, String cssclass, ChartOptions opts, QueryResults res) {
		try {
			//
			// this is quite messy i'm afraid
			//
			opts.getAxisX1().setFormatter(f -> sdf.format(new Date(f.longValue())));

			List<String> fields = new ArrayList<>();
			for (Series s : opts.getSeries()) {
				fields.add(s.getField());
				if (s.getSubfields() != null)
					fields.addAll(s.getSubfields());
			}

			long tsStart = res.getStart();
			long tsLength = res.getEnd() - res.getStart();

			//
			// now onto rendering the chart
			//
			final int AXIS_W = 55;
			final int AXIS_H = 22;
			final int width = opts.getWidth();
			final int height = opts.getHeight();
			int nAxes = (opts.getAxisY2().isEnabled() ? 1 : 0) + (opts.getAxisY1().isEnabled() ? 1 : 0);
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

			int seriesIndex = 0;

			//
			// work out the extent of each axis
			//
			for (Series series : opts.getSeries()) {
				Axis<?> ax = series.getYAxis();
				ax.setMin(Float.MAX_VALUE);
				ax.setMax(Float.MIN_VALUE);
			}
			for (Series series : opts.getSeries()) {
				int fmain = res.getFieldIndex(series.getField());
				Axis<?> ax = series.getYAxis();
				for (QueryRecord rec : res.getRecords()) {
					float f = rec.getFloat(fmain);
					ax.setMin(Math.min(ax.getMin(), f));
					ax.setMax(Math.max(ax.getMax(), f));
				}
			}
			opts.getAxisY1().setMax(opts.getAxisY1().getMax() * 1.05f);
			opts.getAxisY2().setMax(opts.getAxisY2().getMax() * 1.05f);

			for (Series series : opts.getSeries()) {
				++seriesIndex;
				int fmain = res.getFieldIndex(series.getField());
				int[] fsubs = new int[series.getSubfields().size()];
				for (int i = 0; i < fsubs.length; ++i)
					fsubs[i] = res.getFieldIndex(series.getSubfields().get(i));

				Axis<?> axy = series.getYAxis();

				List<String> pathlist = new ArrayList<>();

				//
				// main line
				//
				StringBuilder sb = new StringBuilder();
				sb.append("<path class=\"series" + seriesIndex + "_line\" d=\"");
				float f = 0.0f;
				char ch = 'M';
				for (QueryRecord rec : res.getRecords()) {
					long ts = rec.getLong(fts);
					f = rec.getFloat(fmain);
					sb.append(ch).append(ti(width * (float) (ts - tsStart) / tsLength)).append(" ").append(ti(height - (height * axy.eval(f)))).append(" ");
					ch = 'L';
				}
				sb.append("\" />\n");
				pathlist.add(sb.toString());

				//
				// fill
				//
				sb = new StringBuilder();
				sb.append("<path class=\"series" + seriesIndex + "_fill\" d=\"M0 ").append(height - 0).append(" ");
				f = 0.0f;
				for (QueryRecord rec : res.getRecords()) {
					long ts = rec.getLong(fts);
					f = rec.getFloat(fmain);
					sb.append("L").append(ti(width * (float) (ts - tsStart) / tsLength)).append(" ").append(ti(height - (height * axy.eval(f)))).append(" ");
				}
				sb.append("L").append(width).append(" ").append(height - 0).append(" ");
				sb.append("\" />\n");
				pathlist.add(sb.toString());

				//
				// polygons for each sub-measure, if we have them
				//
				int rcount = res.getRecords().size();
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
						float y = height * axy.eval(f);
						sb.append("L").append(ti(width * (float) (ts - tsStart) / tsLength)).append(" ").append(ti(height - (y + offsets[j]))).append(" ");
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
			if (opts.getAxisY1().isEnabled()) {
				Function<Float, String> f = opts.getAxisY1().getFormatter();
				html.append("<path d=\"M%d %d %d %d\" stroke-width=\"2\" stroke=\"black\"/>\n", AXIS_W, 0, AXIS_W, height - AXIS_H);
				html.append("<text x=\"%d\" y=\"%d\" alignment-baseline=\"hanging\" text-anchor=\"end\" class=\"axis\">%s</text>", AXIS_W - 4, 0, f.apply(opts.getAxisY1().getMax()));
				html.append("<text x=\"%d\" y=\"%d\" text-anchor=\"end\" class=\"axis\">%s</text>", AXIS_W - 4, height - AXIS_H, f.apply(opts.getAxisY1().getMin()));
			}
			if (opts.getAxisY2().isEnabled()) {
				Function<Float, String> f = opts.getAxisY2().getFormatter();
				int w = width - AXIS_W;
				html.append("<path d=\"M%d %d %d %d\" stroke-width=\"2\" stroke=\"black\"/>\n", w, 0, w, height - AXIS_H);
				html.append("<text x=\"%d\" y=\"%d\" alignment-baseline=\"hanging\" text-anchor=\"start\" class=\"axis\">%s</text>", w + 4, 0, f.apply(opts.getAxisY2().getMax()));
				html.append("<text x=\"%d\" y=\"%d\" text-anchor=\"start\" class=\"axis\">%s</text>", w + 4, height - AXIS_H, f.apply(opts.getAxisY2().getMin()));
			}
			Function<Long, String> f2 = opts.getAxisX1().getFormatter();
			html.append("<path d=\"M%d %d %d %d\" stroke-width=\"2\" stroke=\"black\"/>\n", (opts.getAxisY1().isEnabled() ? 1 : 0) * AXIS_W, height - AXIS_H, width - (opts.getAxisY2().isEnabled() ? 1 : 0) * AXIS_W, height - AXIS_H);
			html.append("<text x=\"%d\" y=\"%d\" alignment-baseline=\"hanging\" class=\"axis\">%s</text>", AXIS_W, height - AXIS_H + 4, f2.apply(minX));
			html.append("<text x=\"%d\" y=\"%d\" alignment-baseline=\"hanging\" text-anchor=\"end\" class=\"axis\">%s</text>", width - (AXIS_W * (nAxes - 1)), height - AXIS_H + 4, f2.apply(maxX));

			html.append("<circle cx=\"%d\" cy=\"%d\" r=\"2\" fill=\"black\" />", (opts.getAxisY1().isEnabled() ? 1 : 0) * AXIS_W, height - AXIS_H);
			html.append("<circle cx=\"%d\" cy=\"%d\" r=\"2\" fill=\"black\" />", width - (opts.getAxisY2().isEnabled() ? 1 : 0) * AXIS_W, height - AXIS_H);
			html.append("<circle cx=\"%d\" cy=\"%d\" r=\"2\" fill=\"black\" />", (opts.getAxisY1().isEnabled() ? 1 : 0) * AXIS_W, 0);

			html.append("</svg>\n");
		} catch (Exception e) {
			html.clear();
			html.append("Error: " + e.getMessage());
			LOGGER.error(e);
		}
	}

	private static List<String> list(String... strings) {
		List<String> a = new ArrayList<>();
		for (String s : strings)
			a.add(s);
		return a;
	}

}
