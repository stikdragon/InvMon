package uk.co.stikman.invmon.datamodel;

public class FieldCounts {
	public int	ints;
	public int	strings;
	public int	floats;
	public int	bytes;

	public int getAndInc(FieldDataType dt) {
		switch (dt) {
			case FLOAT:
				return floats++;
			case INT:
				return ints++;
			case STRING:
				return strings++;
			case FLOAT8:
				return bytes++;
			default:
				throw new IllegalArgumentException();
		}
	}

}
