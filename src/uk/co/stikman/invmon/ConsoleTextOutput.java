package uk.co.stikman.invmon;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;

import uk.co.stikman.invmon.inverter.InvUtil;

public class ConsoleTextOutput {
	public static final String		RESET				= "\u001B[0m";

	public static final String		BLACK				= "\u001B[30m";
	public static final String		RED					= "\u001B[31m";
	public static final String		GREEN				= "\u001B[32m";
	public static final String		YELLOW				= "\u001B[33m";
	public static final String		BLUE				= "\u001B[34m";
	public static final String		PURPLE				= "\u001B[35m";
	public static final String		CYAN				= "\u001B[36m";
	public static final String		WHITE				= "\u001B[37m";

	public static final String		BRIGHT_BLACK		= "\u001B[30;1m";
	public static final String		BRIGHT_RED			= "\u001B[31;1m";
	public static final String		BRIGHT_GREEN		= "\u001B[32;1m";
	public static final String		BRIGHT_YELLOW		= "\u001B[33;1m";
	public static final String		BRIGHT_BLUE			= "\u001B[34;1m";
	public static final String		BRIGHT_PURPLE		= "\u001B[35;1m";
	public static final String		BRIGHT_CYAN			= "\u001B[36;1m";
	public static final String		BRIGHT_WHITE		= "\u001B[37;1m";

	private static final String		CLEAR				= "\033[H\033[2J";
	private static final String		MOVE_TOPLEFT		= "\033[H";
	private static final String		HIDE_CURSOR			= "\u001B[?25l";
	private static final String		SHOW_CURSOR			= "\u001B[?25h";
	private PrintStream				output;
	private PrintStream				target;
	private boolean					enableControlCodes	= true;

	private ByteArrayOutputStream	outputBuffer;

	public ConsoleTextOutput(PrintStream output) {
		this.target = output;
		this.output = output;
	}

	public ConsoleTextOutput clear() {
		if (enableControlCodes)
			output.print(CLEAR);
		return this;
	}

	public ConsoleTextOutput println(String s) {
		output.println(s);
		return this;
	}

	public ConsoleTextOutput print(String s) {
		output.print(s);
		return this;
	}

	public ConsoleTextOutput printFloat(float f, int digits, int dp) {
		return printFloat(f, digits, dp, null);
	}

	public ConsoleTextOutput printFloat(float f, int digits, int dp, String suffix) {
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(dp);
		df.setMinimumFractionDigits(dp);
		df.setGroupingUsed(false);
		String s = df.format(f);

		String pad = InvUtil.stringOfChar(digits - s.length(), ' ');

		if (pad.length() > 0) {
			if (enableControlCodes)
				output.print(BRIGHT_BLACK);
			output.print(pad);
		}
		if (enableControlCodes)
			output.print(BRIGHT_YELLOW);
		output.print(s);
		if (enableControlCodes)
			output.print(RESET);
		if (suffix != null)
			output.print(suffix);
		return this;
	}

	public ConsoleTextOutput printInt(float f, int digits) {
		return printInt(f, digits, null);
	}

	public ConsoleTextOutput printInt(float f, int digits, String suffix) {
		int n = (int) f;
		String s = Integer.toString(n);
		String pad = InvUtil.stringOfChar(digits - s.length(), ' ');

		if (pad.length() > 0) {
			if (enableControlCodes)
				output.print(BRIGHT_BLACK);
			output.print(pad);
		}
		if (enableControlCodes)
			output.print(BRIGHT_YELLOW);
		output.print(s);
		if (enableControlCodes)
			output.print(RESET);
		if (suffix != null)
			output.print(suffix);
		return this;
	}

	public void newline() {
		output.println();
	}

	public void moveTopLeft() {
		if (enableControlCodes)
			output.print(MOVE_TOPLEFT);
	}

	public void hideCursor() {
		if (enableControlCodes)
			output.print(HIDE_CURSOR);
	}

	public void showCursor() {
		if (enableControlCodes)
			output.print(SHOW_CURSOR);
	}

	public ConsoleTextOutput color(String code) {
		if (enableControlCodes)
			output.print(code);
		return this;
	}

	public ConsoleTextOutput reset() {
		if (enableControlCodes)
			output.print(RESET);
		return this;
	}

	public ConsoleTextOutput spaces(int n) {
		while (n-- > 0)
			output.print(" ");
		return this;
	}

	public void beginFrame() {
		if (outputBuffer != null)
			throw new IllegalStateException("endFrame() has not been called");
		outputBuffer = new ByteArrayOutputStream();
		output = new PrintStream(outputBuffer);
	}

	public void endFrame() {
		if (outputBuffer == null)
			throw new IllegalStateException("beginFrame() has not been called");
		try {
			output.flush();
			outputBuffer.flush();

			target.print(new String(outputBuffer.toByteArray(), StandardCharsets.UTF_8));
			target.println();
			target.flush();
			outputBuffer = null;
			output = target;
		} catch (IOException e) {
			throw new RuntimeException("IOException: " + e.getMessage(), e);
		}
	}

	public boolean isEnableControlCodes() {
		return enableControlCodes;
	}

	public void setEnableControlCodes(boolean enableControlCodes) {
		this.enableControlCodes = enableControlCodes;
	}

}
