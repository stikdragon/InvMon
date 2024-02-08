package uk.co.stikman.invmon.shared;

import java.util.ArrayList;
import java.util.List;

public class OptionEnum extends Option {

	private String			value;
	private List<String>	values;

	public <T extends Enum<T>> OptionEnum(String name, String display, String value, Class<T> eenum) {
		super(name, display);
		this.value = value;
		this.values = new ArrayList<>();
		for (T x : eenum.getEnumConstants())
			values.add(x.name());
	}

	public OptionEnum(String name, String display, String value, List<String> values) {
		super(name, display);
		this.value = value;
		this.values = new ArrayList<>(values);
	}
	
	public OptionEnum(String name, String display, String value, String csv) {
		super(name, display);
		this.value = value;
		this.values = new ArrayList<>();
		for (String s : csv.split(","))
			values.add(s.trim());
	}

	@Override
	public OptionType getType() {
		return OptionType.ENUM;
	}

	public String getValue() {
		return value;
	}

	public List<String> getValues() {
		return values;
	}

}
