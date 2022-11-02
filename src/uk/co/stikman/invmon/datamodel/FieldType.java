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
	FREQ
	
}
