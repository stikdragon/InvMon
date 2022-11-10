package uk.co.stikman.invmon.datamodel;

public class FieldCounts {
	public int	ints;
	public int	strings;
	public int	floats;

	public int getAndInc(FieldDataType dt) {
		switch (dt) {
			case FLOAT:
				return floats++;
			case INT:
				return ints++;
			case STRING:
				return strings++;
			default:
				throw new IllegalArgumentException();
		}
	}

}
