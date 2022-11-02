package uk.co.stikman.invmon;

import uk.co.stikman.invmon.inverter.BatteryChargeStage;

/**
 * a general purpose data point, contains a bunch of values associated with
 * fields in the model
 * 
 * @author stikd
 *
 */
public class DataPoint {
	private long				timestamp;
	private VIFReading			load	= VIFReading.EMPTY;
	private VIFReading[]		pv		= new VIFReading[0];
	private VIFReading			grid	= VIFReading.EMPTY;
	private VIFReading			battery	= VIFReading.EMPTY;
	private float				stateOfCharge;
	private InverterMode		mode;
	private float				temperature;
	private int					busVoltage;
	private float				loadPF;
	private BatteryChargeStage	chargeState;
	private String				misc;

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public VIFReading getLoad() {
		return load;
	}

	public void setLoad(VIFReading load) {
		this.load = load;
	}

	public VIFReading getPv(int idx) {
		return pv[idx];
	}

	public void setPv(int idx, VIFReading pv) {
		this.pv[idx] = pv;
	}

	public VIFReading getGrid() {
		return grid;
	}

	public void setGrid(VIFReading grid) {
		this.grid = grid;
	}

	public VIFReading getBattery() {
		return battery;
	}

	public void setBattery(VIFReading battery) {
		this.battery = battery;
	}

	public InverterMode getMode() {
		return mode;
	}

	public void setMode(InverterMode mode) {
		this.mode = mode;
	}

	public void setPvCount(int n) {
		pv = new VIFReading[n];
	}

	public int getPvCount() {
		return pv.length;
	}

	public float getTemperature() {
		return temperature;
	}

	public void setTemperature(float temperature) {
		this.temperature = temperature;
	}

	public void setBusVoltage(int v) {
		this.busVoltage = v;
	}

	public float getLoadPF() {
		return loadPF;
	}

	public void setLoadPF(float loadPF) {
		this.loadPF = loadPF;
	}

	public int getBusVoltage() {
		return busVoltage;
	}

	public void setChargeState(BatteryChargeStage x) {
		this.chargeState = x;
	}

	public BatteryChargeStage getChargeState() {
		return chargeState;
	}

	public float getStateOfCharge() {
		return stateOfCharge;
	}

	/**
	 * 0.0 to 1.0
	 * 
	 * @param stateOfCharge
	 */
	public void setStateOfCharge(float stateOfCharge) {
		this.stateOfCharge = stateOfCharge;
	}

	public String getMisc() {
		return misc;
	}

	public void setMisc(String misc) {
		this.misc = misc;
	}

}
