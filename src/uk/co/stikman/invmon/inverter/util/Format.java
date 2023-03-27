package uk.co.stikman.invmon.inverter.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * a very simple implementation of String.format with some custom features
 * 
 * why am i doing this
 * 
 * @author stik
 *
 */
public class Format {

	private List<Inst> instructions = new ArrayList<>();

	private static abstract class Inst {
		public abstract String render(Object arg);

		public abstract boolean acceptsArg();

	}

	public Format(String pattern) {
		StringIter iter = new StringIter(pattern);
		InstLit lit = new InstLit();

		while (iter.hasNext()) {
			char ch = iter.peek();
			if (ch == '%') {
				instructions.add(lit);
				iter.next();
				parseInstruction(iter);
				lit = new InstLit();
			} else
				lit.append(iter.next());
		}
		if (!lit.getValue().toString().isEmpty())
			instructions.add(lit);
	}

	public static String go(String fmt, Object... args) {
		return new Format(fmt).format(args);
	}

	private void parseInstruction(StringIter iter) {
		Inst in = null;
		//
		// allowed [<width>][.<precision>]<type>
		//
		boolean seenDot = false;
		boolean hasPrecision = false;
		boolean hasWidth = false;
		int precision = 0;
		int width = 0;
		while (iter.hasNext()) {
			char ch = iter.next();
			if (isDigit(ch)) {
				if (seenDot) {
					hasPrecision = true;
					precision = precision * 10 + (ch - '0');
				} else {
					hasWidth = true;
					width = width * 10 + (ch - '0');
				}
			} else if (ch == '.') {
				seenDot = true;
			} else if (ch == 'f') {
				in = new FloatInstruction(hasWidth ? width : -1, hasPrecision ? precision : -1);
				break;
			} else if (ch == 'd') {
				if (hasPrecision)
					throw new IllegalArgumentException("%d cannot specify a precision\n" + iter.toString());
				in = new DecimalInstruction(hasWidth ? width : -1);
				break;
			} else if (ch == 'Y') {
				if (hasPrecision || hasWidth)
					throw new IllegalArgumentException("%t/%T cannot specify a precision or width\n" + iter.toString());
				in = new TimeInstruction("yyy-MM-dd HH:mm");
				break;
			} else if (ch == 't' || ch == 'T') {
				if (hasPrecision || hasWidth)
					throw new IllegalArgumentException("%t/%T cannot specify a precision or width\n" + iter.toString());
				if (ch == 't')
					in = new TimeInstruction("HH:mm");
				else
					in = new TimeInstruction("yyyy-MM-dd");

				break;
			} else
				throw new IllegalArgumentException("Unexpected format string:\n" + iter.toString());
		}

		instructions.add(in);
	}

	private static boolean isDigit(char ch) {
		return ch >= '0' && ch <= '9';
	}

	private static class DecimalInstruction extends Inst {

		private int width;

		public DecimalInstruction(int width) {
			this.width = width;
		}

		@Override
		public String render(Object arg) {
			Number n = (Number) arg;
			int a = n.intValue();
			if (width == -1)
				return Integer.toString(a);
			return InvUtil.padLeft(Integer.toString(a), width);
		}

		@Override
		public boolean acceptsArg() {
			return true;
		}

	}

	private static class FloatInstruction extends Inst {
		private int	width;
		private int	precision;

		public FloatInstruction(int width, int precision) {
			super();
			this.width = width;
			this.precision = precision;
		}

		@Override
		public String render(Object arg) {
			//
			// this is a bit dumb, doesn't do rounding. don't care :P
			Number n = (Number) arg;
			String s = Integer.toString(n.intValue());
			if (width != -1)
				s = InvUtil.padLeft(s, width);

			float f = n.floatValue() - n.intValue();
			String t = Float.toString(f).substring(2); // will always be 0.123456, so trim off "0."
			if (precision == -1)
				return s + "." + t;

			char[] out = new char[precision];
			for (int i = 0; i < precision; ++i) {
				if (i < t.length())
					out[i] = t.charAt(i);
				else
					out[i] = '0';
			}
			return s + "." + new String(out);
		}

		@Override
		public boolean acceptsArg() {
			return true;
		}

	}

	public class TimeInstruction extends Inst {
		private SimpleDateFormat sdf;

		public TimeInstruction(String fmt) {
			sdf = new SimpleDateFormat(fmt);
		}

		@Override
		public String render(Object arg) {
			Date d;
			if (arg instanceof Number)
				d = new Date(((Number) arg).longValue());
			else
				d = (Date) arg;
			return sdf.format(d);
		}

		@Override
		public boolean acceptsArg() {
			return true;
		}

	}

	private static class InstLit extends Inst {
		private StringBuilder value = new StringBuilder();

		@Override
		public String render(Object arg) {
			return getValue().toString();
		}

		public void append(char ch) {
			getValue().append(ch);
		}

		@Override
		public boolean acceptsArg() {
			return false;
		}

		public StringBuilder getValue() {
			return value;
		}
	}

	public String format(Object... args) {
		StringBuilder output = new StringBuilder();
		int argptr = 0;
		for (Inst in : instructions) {
			if (in.acceptsArg()) {
				output.append(in.render(args[argptr++]));
			} else {
				output.append(in.render(null));
			}
		}

		return output.toString();

	}
}
