package uk.co.stikman.invmon.shared;

public class OptionFloat extends Option {

	private float value;

	public OptionFloat(String name, String display, float val) {
		super(name, display);
		this.value = val;
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

}
