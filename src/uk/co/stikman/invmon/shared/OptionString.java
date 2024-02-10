package uk.co.stikman.invmon.shared;

import org.json.JSONObject;

public class OptionString extends Option {

	private String		value;
	private OptionType	type;

	public OptionString(String name) {
		super(name);
	}

	public OptionString(String name, String display, String curval) {
		this(name, display, curval, OptionType.STRING);
	}

	public OptionString(String name, String display, String curval, OptionType type) {
		super(name, display);
		this.value = curval;
		if (type == OptionType.STRING || type == OptionType.LONG_STRING || type == OptionType.XML)
			this.type = type;
		else
			throw new IllegalArgumentException("Type is not a string");
	}

	@Override
	public OptionType getType() {
		return type;
	}

	public String getValue() {
		return value;
	}

	@Override
	public void toJSON(JSONObject root) {
		super.toJSON(root);
		root.put("value", value);
	}

	@Override
	public void fromJSON(JSONObject root) {
		super.fromJSON(root);
		type = root.getEnum(OptionType.class, "type");
		value = root.getString("value");
	}

	public void setValue(String value) {
		this.value = value;
	}

}
