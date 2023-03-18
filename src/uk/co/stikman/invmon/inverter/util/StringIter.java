package uk.co.stikman.invmon.inverter.util;

public class StringIter {
	private char[]	chars;
	private int		idx;
	private String	string;

	public StringIter(String src) {
		this.chars = src.toCharArray();
		this.string = src;
	}

	public char next() {
		if (idx >= chars.length)
			throw new IndexOutOfBoundsException();
		return chars[idx++];
	}

	public boolean hasNext() {
		return idx < chars.length;
	}

	public void rewind() {
		rewind(1);
	}

	public void rewind(int n) {
		idx -= n;
		if (idx < 0)
			idx = 0;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(string).append("\n");
		for (int i = 0; i < idx; ++i)
			sb.append(" ");
		sb.append("^");
		return sb.toString();
	}

	public char peek() {
		return chars[idx];
	}

}
