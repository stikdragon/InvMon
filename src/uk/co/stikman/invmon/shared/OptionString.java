package uk.co.stikman.invmon.shared;

public class OptionString extends Option {

	private String				value;
	private final OptionType	type;

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

}
