package uk.co.stikman.invmon.inverter.util;

public class StringerInputStream {
	private String	data;
	private int		pos	= 0;

	public StringerInputStream(String data) {
		super();
		this.data = data;
	}

	public String readString() {
		int n = readInt();
		if (n == -1)
			return null;
		if (n == 0)
			return "";

		if (pos + n > data.length())
			throw new IndexOutOfBoundsException("Position " + pos + ", length " + n);
		String s = data.substring(pos, pos + n);
		pos += n;
		if (eof())
			return s;

		//
		// Otherwise there should be a separator here
		//
		++pos;
		return s;
	}

	private final boolean eof() {
		return pos >= data.length();
	}

	/**
	 * Read until the next comma, returning that string. Stream position is left on
	 * the start of the next value (check {@link #eof()} is not <code>true</code>)
	 * 
	 * @return
	 */
	private final String readNextValue() {
		if (eof())
			throw new IndexOutOfBoundsException();

		int start = pos;
		while (pos < data.length()) {
			if (data.charAt(pos) == ',')
				return data.substring(start, pos++);
			++pos;
		}
		return data.substring(start);
	}

	public int readInt() {
		String s = readNextValue();
		if ("-".equals(s))
			return 0;
		return Integer.parseInt(s, 16);
	}

	public long readLong() {
		return Long.parseLong(readNextValue(), 16);
	}

	public float readFloat() {
		return Float.parseFloat(readNextValue());
	}

	public double readDouble() {
		return Double.parseDouble(readNextValue());
	}

	public boolean readBoolean() {
		String s = readNextValue();
		if ("-".equals(s))
			return false;
		if ("0".equals(s))
			return false;
		if ("1".equals(s))
			return true;
		throw new RuntimeException("Invalid boolean value: " + s);
	}

	public <X extends Enum<X>> X readEnum(Class<X> cls) {
		String s = readNextValue();
		if ("-".equals(s))
			return null;
		return cls.getEnumConstants()[Integer.parseInt(s, 16)];
	}

	public String getData() {
		return data;
	}

	public int getPos() {
		return pos;
	}

	public String readRawString() {
		return readNextValue();
	}

}