package uk.co.stikman.invmon.datamodel;

public enum FieldDataType {
	STRING,
	INT,
	FLOAT;

	/**
	 * size, in bytes
	 * 
	 * @return
	 */
	public int getTypeSize() {
		switch (this) {
			case FLOAT:
			case INT:
				return 4;
			case STRING:
				return 4;
			default:
				throw new IllegalArgumentException("what");
		}
	}

}
