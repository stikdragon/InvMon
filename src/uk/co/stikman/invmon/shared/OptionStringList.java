package uk.co.stikman.invmon.shared;

import java.util.List;

public class OptionStringList extends Option {

	private List<String> value;

	public OptionStringList(String name, String display, List<String> value) {
		super(name, display);
		this.value = value;
	}

	@Override
	public OptionType getType() {
		return OptionType.STRING_LIST;
	}

	public List<String> getValue() {
		return value;
	}

}
