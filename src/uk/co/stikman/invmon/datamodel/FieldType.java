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

	public FieldDataType getBaseType() {
		switch (this) {
			case BOOLEAN:
				return FieldDataType.INT;
			case CURRENT:
			case FLOAT:
			case FREQ:
			case INT:
			case POWER:
			case VOLTAGE:
				return FieldDataType.FLOAT;
			case STRING:
				return FieldDataType.STRING;
			case TIMESTAMP: // timestamp is a special field
				return null;
			default:
				throw new IllegalArgumentException("what");
		}
	}

}
