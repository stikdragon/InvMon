package uk.co.stikman.invmon.datamodel;

import java.util.HashMap;
import java.util.Map;

public class FieldType {
	private static final Map<String, FieldType>	TYPES		= new HashMap<>();
	public static final FieldType				TIMESTAMP	= new FieldType("TIMESTAMP", null, 0, 0, 0);

	public static final FieldType				BOOLEAN		= new FieldType("BOOLEAN", FieldDataType.INT, 0, 0, 0);
	public static final FieldType				INT			= new FieldType("INT", FieldDataType.INT, 0, 0, 0);

	public static final FieldType				FLOAT		= new FieldType("FLOAT", FieldDataType.FLOAT, 0, 0, 0);
	public static final FieldType				VOLTAGE		= new FieldType("VOLTAGE", FieldDataType.FLOAT, 0, 0, 0);
	public static final FieldType				CURRENT		= new FieldType("CURRENT", FieldDataType.FLOAT, 0, 0, 0);
	public static final FieldType				POWER		= new FieldType("POWER", FieldDataType.FLOAT, 0, 0, 0);
	public static final FieldType				FREQ		= new FieldType("FREQ", FieldDataType.FLOAT, 0, 0, 0);

	private final float							min;
	private final float							max;
	private final String						name;
	private final FieldDataType					baseType;
	private final int							width;


	private FieldType(String name, FieldDataType basetype, int width, float min, float max) {
		if (TYPES.containsKey(name))
			throw new IllegalArgumentException("Type [" + name + "] already declared");
		TYPES.put(name, this);
		this.name = name;
		this.width = width;
		this.baseType = basetype;
		this.min = min;
		this.max = max;
	}

	public static FieldType createStringType(String name, int width) {
		return new FieldType(name, FieldDataType.STRING, width, 0, 0);
	}

	public FieldDataType getBaseType() {
		return this.baseType;
	}

	public float min() {
		return min;
	}

	public float max() {
		return max;
	}

	public String name() {
		return name;
	}

	public int width() {
		return width;
	}

	public static FieldType parse(String type) {
		FieldType t = TYPES.get(type);
		if (t != null)
			return t;

		//
		// now do some special cases
		//
		String[] bits = type.split(",");
		if (bits[0].trim().equalsIgnoreCase("string"))
			return new FieldType(type.toUpperCase(), FieldDataType.STRING, Integer.parseInt(bits[1]), 0, 0);
		else if (bits[0].trim().equalsIgnoreCase("volt8"))
			return new FieldType(type.toUpperCase(), FieldDataType.FLOAT8, 0, Float.parseFloat(bits[1]), Float.parseFloat(bits[2]));

		throw new IllegalArgumentException("Type [" + type.toUpperCase() + "] is not recognised");
	}

	@Override
	public String toString() {
		return name;
	}
}
