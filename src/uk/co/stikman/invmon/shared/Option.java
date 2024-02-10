package uk.co.stikman.invmon.shared;

import org.json.JSONObject;

public abstract class Option {
	private final String	name;
	private String			display;

	public Option(String name) {
		this(name, null);
	}

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

	public void toJSON(JSONObject root) {
		root.put("display", display);
		root.put("type", getType().name());
	}

	public void fromJSON(JSONObject root) {
		display = root.getString("display");
	}

}
