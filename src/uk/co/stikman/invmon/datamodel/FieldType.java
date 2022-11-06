package uk.co.stikman.invmon.datamodel;

public enum FieldType {
	TIMESTAMP,
	STRING,
	INT,
	FLOAT,
	BOOLEAN,

	//
	// These are all floats, but with context
	//
	VOLTAGE,
	CURRENT,
	POWER,
	FREQ;

	/**
	 * size, in bytes
	 * 
	 * @return
	 */
	public int getTypeSize() {
		switch (this) {
			case BOOLEAN:
				return 1;
			case CURRENT:
			case FLOAT:
			case FREQ:
			case INT:
			case POWER:
			case VOLTAGE:
				return 4;
			case STRING:
				return -1;
			case TIMESTAMP:
				return 8;
			default:
				throw new IllegalArgumentException("what");
		}
	}

	public FieldType getBaseType() {
		switch (this) {
			case BOOLEAN:
				return BOOLEAN;
			case CURRENT:
			case FLOAT:
			case FREQ:
			case INT:
			case POWER:
			case VOLTAGE:
				return FLOAT;
			case STRING:
				return STRING;
			case TIMESTAMP:
				return TIMESTAMP;
			default:
				throw new IllegalArgumentException("what");
		}
	}

}
