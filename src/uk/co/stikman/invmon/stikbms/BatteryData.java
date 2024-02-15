package uk.co.stikman.invmon.stikbms;

import java.util.Arrays;

public class BatteryData {

	private final int	id;

	private float[]		cellVoltages;
	private float		current;
	private float		temperature;
	private float		packVoltage;

	public BatteryData(int id, int cellsPerBatt) {
		super();
		this.id = id;
		this.cellVoltages = new float[cellsPerBatt];
	}

	public float[] getCellVoltages() {
		return cellVoltages;
	}

	public float getCurrent() {
		return current;
	}

	public void setCurrent(float current) {
		this.current = current;
	}

	public float getTemperature() {
		return temperature;
	}

	public void setTemperature(float temperature) {
		this.temperature = temperature;
	}

	public int getId() {
		return id;
	}

	@Override
	public String toString() {
		return "BatteryData [id=" + id + ", cellVoltages=" + Arrays.toString(cellVoltages) + ", current=" + current + ", temperature=" + temperature + "]";
	}

	public float getPackVoltage() {
		return packVoltage;
	}

	public void setPackVoltage(float packVoltage) {
		this.packVoltage = packVoltage;
	}

}
