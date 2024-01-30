package uk.co.stikman.invmon.stikbms;

public class BMSMetrics {
	private float[]	voltages;
	private float	current;
	private float[]	temperatures;

	public float[] getVoltages() {
		return voltages;
	}

	public void setVoltages(float[] voltages) {
		this.voltages = voltages;
	}

	public float getCurrent() {
		return current;
	}

	public void setCurrent(float current) {
		this.current = current;
	}

	public float[] getTemperatures() {
		return temperatures;
	}

	public void setTemperatures(float[] temperatures) {
		this.temperatures = temperatures;
	}

}
