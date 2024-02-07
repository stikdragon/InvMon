package uk.co.stikman.invmon.client;

public class CharIter {

	private final String	s;
	private int				pos	= -1;

	public CharIter(String s) {
		if (s == null)
			this.s = "";
		else
			this.s = s;
	}

	public boolean hasNext() {
		return pos < s.length() - 1;
	}

	public char next() {
		if (pos >= s.length() - 1)
			throw new IllegalStateException("End of text");
		return s.charAt(++pos);
	}

	public char peek() {
		if (pos >= s.length() - 1)
			return 0;
		return s.charAt(pos + 1);
	}

	public int getPosition() {
		return pos;
	}
}
