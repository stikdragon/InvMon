package uk.co.stikman.invmon.server.widgets;

import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.minidom.MDElement;
import uk.co.stikman.invmon.server.HTMLBuilder;
import uk.co.stikman.invmon.server.InvMonClientError;
import uk.co.stikman.invmon.server.PageLayout;
import uk.co.stikman.invmon.server.UserSesh;
import uk.co.stikman.invmon.server.WebUtils;
import uk.co.stikman.invmon.shared.OptionEnum;
import uk.co.stikman.invmon.shared.OptionFloat;
import uk.co.stikman.invmon.shared.OptionString;
import uk.co.stikman.invmon.shared.OptionType;
import uk.co.stikman.invmon.shared.WidgetConfigOptions;
import uk.co.stikman.log.StikLog;

public class GaugeWidget extends PageWidget {

	private static final StikLog LOGGER = StikLog.getLogger(GaugeWidget.class);

	public enum Mode {
		NORMAL, SPLIT
	}

	public enum ValueFormat {
		NORMAL, PERCENT, CURRENT, VOLTAGE
	}

	private ColourBandingOptions	colours	= new ColourBandingOptions();
	private Mode					mode	= Mode.NORMAL;
	private String					fieldname;
	private float					rangeMin;
	private float					rangeMax;
	private float					arcsize	= 200.0f;
	private ValueFormat				valueFormat;

	public GaugeWidget(PageLayout owner) {
		super(owner);
	}

	@Override
	public JSONObject executeApi(UserSesh sesh, String api, JSONObject args) {
		try {
			ensureCachedResults(sesh);
			DBRecord rec = sesh.getData(WebUtils.CACHED_LAST_RECORD);
			if (rec == null)
				throw new InvMonClientError("No data");

			Field f = getOwner().getEnv().getModel().get(fieldname);
			float src = rec.getFloat(f);
			float value = src;
			//
			// normalise this 
			//
			if (rangeMin == rangeMax) {
				value = 0.0f;
			} else {
				value -= rangeMin;
				value /= (rangeMax - rangeMin);
			}

			value = InvUtil.clamp(value, 0.0f, 1.0f);

			JSONObject jo = new JSONObject();
			HTMLBuilder html = new HTMLBuilder();
			html.append("<div class=\"gauge\">");
			html.append(render(value));
			html.append("<div class=\"reading\">");

			String s;
			switch (valueFormat) {
				case CURRENT:
					s = String.format("%.2fA", (float) src);
					break;
				case VOLTAGE:
					s = String.format("%.2fV", (float) src);
					break;
				case NORMAL:
					s = String.format("%.2f", (float) src);
					break;
				case PERCENT:
					s = (int) (src * 100.0f) + "%";
					break;
				default:
					s = "?";
					break;
			}

			html.append(s);
			html.append("</div>");
			html.append("</div>");

			jo.put("html", html.toString());
			return jo;
		} catch (Exception e) {
			LOGGER.error(e);
			throw new RuntimeException("Error rendering daily summary", e);
		}
	}

	private String render(float value) {

		//
		// draw an arc as a bunch of segments
		//
		float ything = 0.0f; // y-thing represents the "squashed-ness" of it, very short arc lengths make it go toward 1.0
		ything = InvUtil.clamp(1.0f - (arcsize - 50.0f) / 50.0f, 0.0f, 1.0f);
		float startAng = 180 - arcsize / 2f;
		float endAng = 180 + arcsize / 2f;
		float gapAng = 0.5f;
		int segments = 20;
		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float maxX = 50.0f; // 50 is the midpoint
		float maxY = 50.0f;

		//
		// we'll scale the maxY back a bit if we're a short arc length
		//
		float r0 = 0.7f + ything * 0.2f;
		float r1 = 1.0f;
		maxY -= 45.0f * ything;

		HTMLBuilder html = new HTMLBuilder();
		html.append("<svg viewBox=\"VIEWBOX\">");
		html.append("<g>");
		for (int seg = 0; seg < segments; ++seg) {
			float a0 = (3.14159f * 2.0f / 360f) * ((startAng + (endAng - startAng) * seg / segments) + gapAng);
			float a1 = (3.14159f * 2.0f / 360f) * ((startAng + (endAng - startAng) * (seg + 1) / segments) - gapAng);

			float x0 = 50.0f * (float) (r0 * -Math.sin(a0) + 1.0f);
			float x1 = 50.0f * (float) (r1 * -Math.sin(a0) + 1.0f);
			float x2 = 50.0f * (float) (r1 * -Math.sin(a1) + 1.0f);
			float x3 = 50.0f * (float) (r0 * -Math.sin(a1) + 1.0f);

			float y0 = 50.0f * (float) (r0 * Math.cos(a0) + 1.0f);
			float y1 = 50.0f * (float) (r1 * Math.cos(a0) + 1.0f);
			float y2 = 50.0f * (float) (r1 * Math.cos(a1) + 1.0f);
			float y3 = 50.0f * (float) (r0 * Math.cos(a1) + 1.0f);

			minX = min(minX, x0, x1, x2, x3);
			minY = min(minY, y0, y1, y2, y3);
			maxX = max(maxX, x0, x1, x2, x3);
			maxY = max(maxY, y0, y1, y2, y3);

			String c = colours.eval((float) seg / segments);
			html.append("<path fill=\"" + c + "\" stroke=\"none\" d=\"");
			html.append(String.format("M %.3f %.3f L %.3f %.3f L %.3f %.3f L %.3f %.3f", x0, y0, x1, y1, x2, y2, x3, y3));
			html.append("\" />");
		}

		html.append("</g>");

		//
		// do the needle.  need to work out the full deflection angle, and the start.  this
		// time it's in degrees, because it's for the svg engine
		//
		float rot = value * arcsize + startAng - 90f; // the -90 is because our basis is (0,1) and svg is (1,0)
		html.append("<g transform=\"rotate(" + rot + " 50 50)\" fill=\"000000\">");
		html.append("<path class=\"needle\" d=\"M " + (1.0f - ything) * 10 + " 50 L 50 47 L 50 53\" />");
		html.append("<circle class=\"needle\" fill=\"000000\" cx=\"50\" cy=\"50\" r=\"6\" />");
		html.append("</g>");

		html.append("</svg>");

		String s = html.toString();
		s = s.replace("VIEWBOX", String.format("%.3f %.3f %.3f %.3f", minX, minY, maxX - minX, maxY - minY));
		return s;

	}

	private float min(float... vals) {
		float r = vals[0];
		for (float f : vals)
			if (f < r)
				r = f;
		return r;
	}

	private float max(float... vals) {
		float r = vals[0];
		for (float f : vals)
			if (f > r)
				r = f;
		return r;
	}

	@Override
	public String getClientWidgetType() {
		return "serverside";
	}

	@Override
	public void configure(MDElement root) throws IllegalArgumentException {
		super.configure(root);
		fieldname = root.getAttrib("field");
		mode = Mode.valueOf(root.getAttrib("mode", "normal").toUpperCase());
		String s = root.getAttrib("range");
		int p = s.indexOf(',');
		if (p == -1)
			throw new IllegalArgumentException("Range for gauge widget [" + getId() + "] is not correct");
		rangeMin = Float.parseFloat(s.substring(0, p));
		rangeMax = Float.parseFloat(s.substring(p + 1));
		valueFormat = ValueFormat.valueOf(root.getAttrib("text", "normal").toUpperCase());
		arcsize = Float.parseFloat(root.getAttrib("arclength", "210"));

		colours = new ColourBandingOptions();
		switch (mode) {
			case SPLIT:
				colours.addBand(0.00f, 0.50f, 0xff0000, 0xcccccc);
				colours.addBand(0.50f, 1.00f, 0xcccccc, 0x00ff00);
				break;
			case NORMAL:
				colours.addBand(0.00f, 0.23f, 0x0000bf, 0x0000bf);
				colours.addBand(0.23f, 0.45f, 0x0000bf, 0x00bfbf);
				colours.addBand(0.45f, 0.67f, 0x00bfbf, 0x00bf00);
				colours.addBand(0.67f, 1.00f, 0x00bf00, 0x00bf00);
				break;
			default:
				break;

		}

	}

	@Override
	public WidgetConfigOptions getConfigOptions() {
		WidgetConfigOptions wco = new WidgetConfigOptions();
		wco.add(new OptionString("fldname", "Field Name", fieldname, OptionType.STRING));
		wco.add(new OptionEnum("mode", "Mode", mode.name(), "NORMAL,SPLIT"));
		wco.add(new OptionFloat("min", "Lower Range", rangeMin));
		wco.add(new OptionFloat("max", "Upper Range", rangeMax));
		wco.add(new OptionFloat("arclen", "Arc Length", arcsize));
		return wco;
	}

	@Override
	public void applyConfigOptions(WidgetConfigOptions opts) {
		fieldname = opts.get("fldname", OptionString.class).getValue();
		mode = Mode.valueOf(opts.get("mode", OptionEnum.class).getValue());
		rangeMin = opts.get("min", OptionFloat.class).getValue();
		rangeMax = opts.get("max", OptionFloat.class).getValue();
		arcsize = opts.get("arclen", OptionFloat.class).getValue();
	}

}
