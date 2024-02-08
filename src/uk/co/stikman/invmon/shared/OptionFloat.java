package uk.co.stikman.invmon.shared;

import org.json.JSONObject;

public class OptionFloat extends Option {

	private float value;

	public OptionFloat(String name, String display, float val) {
		super(name, display);
		this.value = val;
	}

	public OptionFloat(String name) {
		super(name);
	}

	@Override
	public OptionType getType() {
		return OptionType.FLOAT;
	}

	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		this.value = value;
	}

	@Override
	public void toJSON(JSONObject root) {
		super.toJSON(root);
		root.put("value", value);
	}

	@Override
	public void fromJSON(JSONObject root) {
		super.fromJSON(root);
		value = root.getFloat("value");
	}

}
