package uk.co.stikman.invmon.server;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.datalog.MiniDbException;
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

	/**
	 * "to-int"
	 * 
	 * @param f
	 * @return
	 */
	private static final String ti(float f) {
		return Integer.toString((int) f);
	}

	public static void renderChart(HTMLBuilder html, String cssclass, ChartOptions opts, DataSet res) {
		try {
			//
			// this is quite messy i'm afraid
			//
			opts.getAxisX1().setFormat("%Y", res.getZone());

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
			final int width = opts.getWidth();
			final int height = opts.getHeight();

			int nAxes = (opts.getAxisY2().isEnabled() ? 1 : 0) + (opts.getAxisY1().isEnabled() ? 1 : 0);
			float axisWidthTotal = (opts.getAxisY1().axisSize()) + (opts.getAxisY2().axisSize());
			float sx = 1.0f - (float) axisWidthTotal / width; // scale factors
			float sy = 1.0f - (float) (opts.getAxisX1().axisSize()) / height;
			html.append("<svg class=\"chart ").append(cssclass).append("\" width=\"%dpx\" height=\"%dpx\">\n", width, height);
			html.append("<g transform=\"translate(%d,0) scale(%s, %f)\"> \n", (opts.getAxisY1().axisSize()), sx, sy);
			html.append("<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" class=\"background\" />", 0, 0, width, height);
			int fts = res.getFieldIndex("TIMESTAMP");

			long minX = Long.MAX_VALUE;
			long maxX = Long.MIN_VALUE;
			for (DataSetRecord rec : res.getRecords()) {
				minX = Math.min(minX, rec.getLong(fts));
				maxX = Math.max(maxX, rec.getLong(fts));
			}
			opts.getAxisX1().setMin(minX);
			opts.getAxisX1().setMax(maxX);

			int seriesIndex = 0;

			//
			// work out the extent of each axis
			//
			for (Series series : opts.getSeries()) {
				Axis ax = opts.getAxis(series.getYAxisId());
				ax.setMin(Float.MAX_VALUE);
				ax.setMax(Float.MIN_VALUE);
			}
			for (Series series : opts.getSeries()) {
				int fmain = res.getFieldIndex(series.getField());
				Axis ax = opts.getAxis(series.getYAxisId());
				for (DataSetRecord rec : res.getRecords()) {
					float f = rec.getFloat(fmain);
					ax.setMin(Math.min(ax.getMin(), f));
					ax.setMax(Math.max(ax.getMax(), f));
				}
			}

			if (opts.getAxisY1().getForceMax() == null)
				opts.getAxisY1().setMax(opts.getAxisY1().getMax() * 1.05f);
			if (opts.getAxisY2().getForceMax() == null)
				opts.getAxisY2().setMax(opts.getAxisY2().getMax() * 1.05f);

			for (Series series : opts.getSeries()) {
				++seriesIndex;
				int fmain = res.getFieldIndex(series.getField());
				int[] fsubs = new int[series.getSubfields().size()];
				for (int i = 0; i < fsubs.length; ++i)
					fsubs[i] = res.getFieldIndex(series.getSubfields().get(i));

				Axis axy = opts.getAxis(series.getYAxisId());

				List<String> pathlist = new ArrayList<>();

				//
				// main line
				//
				StringBuilder sb = new StringBuilder();
				sb.append("<path class=\"series" + seriesIndex + "_line\" d=\"");
				float f = 0.0f;
				char ch = 'M';
				for (DataSetRecord rec : res.getRecords()) {
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
				for (DataSetRecord rec : res.getRecords()) {
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
					for (DataSetRecord rec : res.getRecords()) {
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
			int axw = opts.getAxisY1().axisSize();
			int axh = opts.getAxisX1().axisSize();
			if (opts.getAxisY1().isEnabled()) {
				Function<Number, String> f = opts.getAxisY1().getFormatter();
				html.append("<path d=\"M%d %d %d %d\" stroke-width=\"2\" stroke=\"black\"/>\n", axw, 0, axw, height - axh);
				html.append("<text x=\"%d\" y=\"%d\" alignment-baseline=\"hanging\" text-anchor=\"end\" class=\"axis\">%s</text>", axw - 4, 0, f.apply(opts.getAxisY1().getMax()));
				html.append("<text x=\"%d\" y=\"%d\" text-anchor=\"end\" class=\"axis\">%s</text>", axw - 4, height - axh, f.apply(opts.getAxisY1().getMin()));
			}
			if (opts.getAxisY2().isEnabled()) {
				Function<Number, String> f = opts.getAxisY2().getFormatter();
				int w = width - axw;
				html.append("<path d=\"M%d %d %d %d\" stroke-width=\"2\" stroke=\"black\"/>\n", w, 0, w, height - axh);
				html.append("<text x=\"%d\" y=\"%d\" alignment-baseline=\"hanging\" text-anchor=\"start\" class=\"axis\">%s</text>", w + 4, 0, f.apply(opts.getAxisY2().getMax()));
				html.append("<text x=\"%d\" y=\"%d\" text-anchor=\"start\" class=\"axis\">%s</text>", w + 4, height - axh, f.apply(opts.getAxisY2().getMin()));
			}
			Function<Number, String> f2 = opts.getAxisX1().getFormatter();
			html.append("<path d=\"M%d %d %d %d\" stroke-width=\"2\" stroke=\"black\"/>\n", (opts.getAxisY1().isEnabled() ? 1 : 0) * axw, height - axh, width - (opts.getAxisY2().isEnabled() ? 1 : 0) * axw, height - axh);
			html.append("<text x=\"%d\" y=\"%d\" alignment-baseline=\"hanging\" class=\"axis\">%s</text>", axw, height - axh + 4, f2.apply(minX));
			html.append("<text x=\"%d\" y=\"%d\" alignment-baseline=\"hanging\" text-anchor=\"end\" class=\"axis\">%s</text>", width - (axw * (nAxes - 1)), height - axh + 4, f2.apply(maxX));

			html.append("<circle cx=\"%d\" cy=\"%d\" r=\"2\" fill=\"black\" />", (opts.getAxisY1().isEnabled() ? 1 : 0) * axw, height - axh);
			html.append("<circle cx=\"%d\" cy=\"%d\" r=\"2\" fill=\"black\" />", width - (opts.getAxisY2().isEnabled() ? 1 : 0) * axw, height - axh);
			html.append("<circle cx=\"%d\" cy=\"%d\" r=\"2\" fill=\"black\" />", (opts.getAxisY1().isEnabled() ? 1 : 0) * axw, 0);

			html.append("</svg>\n");

		} catch (Exception e) {
			html.clear();
			html.append("Error: " + e.getMessage());
			LOGGER.error(e);
		}
	}

	//	private static void copyAxisInfo(AxisInfo out, Axis<?> in, int size, String name) {
	//		out.setMin(in.getMin());
	//		out.setMax(in.getMax());
	//		out.setSize(size);
	//		out.setName(name);
	//	}

	private static List<String> list(String... strings) {
		List<String> a = new ArrayList<>();
		for (String s : strings)
			a.add(s);
		return a;
	}

}
