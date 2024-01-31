package uk.co.stikman.invmon.stikbms;

public class CalibFactor {
	private String	name;
	private float	value;

	public CalibFactor(String name, float value) {
		super();
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public float getValue() {
		return value;
	}

	@Override
	public String toString() {
		return name + ": " + String.format("%.5f", value);
	}

}
