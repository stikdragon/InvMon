package uk.co.stikman.invmon.inverter.util;

public class StringerOutputStream {
	private StringBuilder	sb	= new StringBuilder();
	private String			sep	= "";

	private void writeRaw(String s) {
		sb.append(sep).append(s);
		sep = ",";
	}

	public StringerOutputStream writeString(String s) {
		if (s == null) {
			writeInt(-1);
		} else if (s.length() == 0) {
			writeInt(0);
		} else {
			writeInt(s.length());
			writeRaw(s);
		}
		return this;
	}

	public StringerOutputStream writeInt(int i) {
		writeRaw(Integer.toString(i, 16));
		return this;
	}

	public StringerOutputStream writeLong(long l) {
		writeRaw(Long.toString(l, 16));
		return this;
	}

	public StringerOutputStream writeFloat(float f) {
		writeRaw(Float.toString(f));
		return this;
	}

	public StringerOutputStream writeDouble(double d) {
		writeRaw(Double.toString(d));
		return this;
	}

	public StringerOutputStream writeBoolean(boolean b) {
		writeRaw(b ? "1" : "0");
		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	public void writeEnum(Enum<?> e) {
		if (e == null)
			writeRaw("-");
		else
			writeRaw(Integer.toString(e.ordinal(), 16));
	}

	public void writeRawString(String s) {
		if (s.indexOf(',') != -1)
			throw new RuntimeException("writeRawString cannot write strings with commas in");
		writeRaw(s);
	}

}
