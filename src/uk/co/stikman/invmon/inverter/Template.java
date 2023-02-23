package uk.co.stikman.invmon.inverter;

import java.util.ArrayList;
import java.util.List;

/**
 * this didn't turn out as clean or clever as i thought :(
 * 
 * @author stik
 *
 */
public class Template {
	private enum BitType {
		LITERAL, MATCH
	}

	private static class Bit {
		BitType	type;
		String	pattern;
		boolean	varLength	= false;

		public String getKey() {
			return pattern.substring(0, 1);
		}

		@Override
		public String toString() {
			return type + ": " + pattern;
		}
	}

	private List<Bit> bits = new ArrayList<>();

	public Template(String fmt) {
		boolean b = false;
		StringBuilder sb = new StringBuilder();
		for (char ch : fmt.toCharArray()) {
			if (ch == '<') {
				if (b)
					throw new IllegalStateException();
				if (sb.length() > 0) {
					Bit bit = new Bit();
					bit.type = BitType.LITERAL;
					bit.pattern = sb.toString();
					bits.add(bit);
				}

				sb = new StringBuilder();
				b = true;
			} else if (ch == '>') {
				if (!b)
					throw new IllegalStateException();
				b = false;
				if (sb.length() == 0)
					throw new IllegalArgumentException();
				Bit bit = new Bit();
				bit.type = BitType.MATCH;
				bit.pattern = sb.toString();
				bit.varLength = bit.pattern.endsWith("+");
				if (bit.varLength)
					bit.pattern = Character.toString(bit.pattern.charAt(0));
				bits.add(bit);

				sb = new StringBuilder();
			} else {
				sb.append(ch);
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		bits.forEach(b -> sb.append(b.toString()).append("\n"));
		return sb.toString();
	}

	/**
	 * throws {@link CommunicationError} if <code>data</code> does not match this
	 * template
	 * 
	 * @param data
	 * @return
	 */
	public TemplateResult apply(String data) {
		TemplateResultImpl x = new TemplateResultImpl();

		int ptr = 0;
		for (Bit bit : bits) {
			if (bit.type == BitType.LITERAL) {
				for (int i = 0; i < bit.pattern.length(); ++i) {
					if (data.charAt(ptr++) != bit.pattern.charAt(i))
						throw new CommunicationError("Response (" + data + ") does not match expected template at position [" + (ptr - 1) + "]");
				}
			} else {
				if (bit.varLength) {
					//
					// keep looking until we get to a space
					//
					int start = ptr;
					while (ptr < data.length()) {
						char ch = data.charAt(ptr++);
						if (ch == ' ') {
							--ptr;
							break;
						}
					}
					x.setPart(bit.getKey(), data.substring(start, ptr));

				} else {
					char[] bits = new char[bit.pattern.length()];
					int k = 0;
					for (int i = 0; i < bit.pattern.length(); ++i) {
						bits[k] = data.charAt(ptr++);
						if (bit.pattern.charAt(i) == '.')
							if (bits[k] != '.')
								throw new CommunicationError("Response (" + data + ") does not match expected template at position [" + (ptr - 1) + "]");
						++k;
					}
					x.setPart(bit.getKey(), new String(bits));
				}
			}
		}

		return x;
	}

}
