package uk.co.stikman.invmon;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Scanner;

import uk.co.stikman.invmon.inverter.InverterUtils;
import uk.co.stikman.log.StikLog;

public class HTMLConsoleThing {
	private StringBuilder			sb;
	private String					curCol			= "white";
	private static final StikLog	LOGGER			= StikLog.getLogger(HTMLConsoleThing.class);
	public static final String		BRIGHT_GREEN	= "green";
	public static final String		BRIGHT_RED		= "red";
	public static final String		BRIGHT_YELLOW	= "yellow";
	public static final String		BRIGHT_BLACK	= "#909090";
	private static final String		WHITE			= "white";

	public void beginFrame() {
		sb = new StringBuilder();

		try (InputStream is = getClass().getResourceAsStream("htmlOutputTop.html")) {
			 Scanner sc = new Scanner(is).useDelimiter("\\A");
			 String s = sc.hasNext() ? sc.next() : "";
			sb.append(s);
			sb.append("<div>");
		} catch (IOException e) {
			LOGGER.error(e);
		}
	}

	public HTMLConsoleThing print(String s) {
		sb.append("<span style=\"color:" + curCol + ";\">").append(s).append("</span>");
		return this;
	}

	public HTMLConsoleThing printFloat(float f, int digits, int dp) {
		return printFloat(f, digits, dp, null);
	}

	public HTMLConsoleThing printFloat(float f, int digits, int dp, String suffix) {
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(dp);
		df.setMinimumFractionDigits(dp);
		df.setGroupingUsed(false);
		String s = df.format(f);

		String pad = InverterUtils.stringOfChar(digits - s.length(), ' ');

		curCol = WHITE;
		if (pad.length() > 0)
			print(pad);

		curCol = BRIGHT_YELLOW;
		print(s);
		curCol = WHITE;
		if (suffix != null)
			print(suffix);
		return this;
	}

	public HTMLConsoleThing color(String c) {
		curCol = c;
		return this;
	}

	public HTMLConsoleThing reset() {
		curCol = WHITE;
		return this;
	}

	public void newline() {
		sb.append("</div>\n<div>");
	}

	public HTMLConsoleThing printInt(float f, int digits, String suffix) {
		int n = (int) f;
		String s = Integer.toString(n);
		String pad = InverterUtils.stringOfChar(digits - s.length(), ' ');

		if (pad.length() > 0)
			print(pad);
		curCol = BRIGHT_YELLOW;
		print(s);
		curCol = WHITE;
		if (suffix != null)
			print(suffix);
		return this;
	}

	public HTMLConsoleThing spaces(int i) {
		// ignore..
		return this;
	}

	public void endFrame() {
		sb.append("</div></body></html>");
	}

	@Override
	public String toString() {
		return sb.toString();
	}
}
