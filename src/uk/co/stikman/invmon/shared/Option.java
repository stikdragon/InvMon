package uk.co.stikman.invmon.shared;

public abstract class Option {
	private final String	name;
	private final String	display;

	public Option(String name, String display) {
		super();
		this.name = name;
		this.display = display;
	}

	public String getName() {
		return name;
	}

	public String getDisplay() {
		return display;
	}

	public abstract OptionType getType();
}
