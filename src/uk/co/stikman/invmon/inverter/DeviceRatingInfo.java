package uk.co.stikman.invmon.inverter;

public class DeviceRatingInfo {
	private float					gridRatingV;
	private float					gridRatingI;
	private float					outputRatingV;
	private float					outputRatingF;
	private float					outputRatingI;
	private int						outputRatingApparentP;
	private int						outputRatingActiveP;
	private float					batteryRatingV;
	private float					batteryRechargeV;
	private float					batteryUnderV;
	private float					batteryBulkV;
	private float					batteryFloatV;
	private BatteryType				batteryType;
	private int						maxGridChargeI;
	private int						maxChargeI;
	private InputVoltageRange		inputVoltageRange;
	private OutputSourcePriority	outputSourcePriority;
	private ChargerSourcePriority	chargerSourcePriority;
	private int						parallelMax;
	private InverterType			machineType;
	private InverterTopology		topology;
	private InverterOutputMode		outputMode;
	private float					batteryReDischargeV;
	private int						maxChargeTimeCV;

	public int getMaxChargeTimeCV() {
		return maxChargeTimeCV;
	}

	public float getGridRatingV() {
		return gridRatingV;
	}

	public float getGridRatingI() {
		return gridRatingI;
	}

	public float getOutputRatingV() {
		return outputRatingV;
	}

	public float getOutputRatingF() {
		return outputRatingF;
	}

	public float getOutputRatingI() {
		return outputRatingI;
	}

	public int getOutputRatingApparentP() {
		return outputRatingApparentP;
	}

	public int getOutputRatingActiveP() {
		return outputRatingActiveP;
	}

	public float getBatteryRatingV() {
		return batteryRatingV;
	}

	public float getBatteryRechargeV() {
		return batteryRechargeV;
	}

	public float getBatteryUnderV() {
		return batteryUnderV;
	}

	public float getBatteryBulkV() {
		return batteryBulkV;
	}

	public float getBatteryFloatV() {
		return batteryFloatV;
	}

	public BatteryType getBatteryType() {
		return batteryType;
	}

	public int getMaxGridChargeI() {
		return maxGridChargeI;
	}

	public int getMaxChargeI() {
		return maxChargeI;
	}

	public InputVoltageRange getInputVoltageRange() {
		return inputVoltageRange;
	}

	public OutputSourcePriority getOutputSourcePriority() {
		return outputSourcePriority;
	}

	public ChargerSourcePriority getChargerSourcePriority() {
		return chargerSourcePriority;
	}

	public int getParallelMax() {
		return parallelMax;
	}

	public InverterType getMachineType() {
		return machineType;
	}

	public InverterTopology getTopology() {
		return topology;
	}

	public InverterOutputMode getOutputMode() {
		return outputMode;
	}

	public float getBatteryReDischargeV() {
		return batteryReDischargeV;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(InverterUtils.padLeft("GridRatingV", 30)).append(": ").append(getGridRatingV()).append("\n");
		sb.append(InverterUtils.padLeft("GridRatingI", 30)).append(": ").append(getGridRatingI()).append("\n");
		sb.append(InverterUtils.padLeft("OutputRatingV", 30)).append(": ").append(getOutputRatingV()).append("\n");
		sb.append(InverterUtils.padLeft("OutputRatingF", 30)).append(": ").append(getOutputRatingF()).append("\n");
		sb.append(InverterUtils.padLeft("OutputRatingI", 30)).append(": ").append(getOutputRatingI()).append("\n");
		sb.append(InverterUtils.padLeft("OutputRatingApparentP", 30)).append(": ").append(getOutputRatingApparentP()).append("\n");
		sb.append(InverterUtils.padLeft("OutputRatingActiveP", 30)).append(": ").append(getOutputRatingActiveP()).append("\n");
		sb.append(InverterUtils.padLeft("BatteryRatingV", 30)).append(": ").append(getBatteryRatingV()).append("\n");
		sb.append(InverterUtils.padLeft("BatteryRechargeV", 30)).append(": ").append(getBatteryRechargeV()).append("\n");
		sb.append(InverterUtils.padLeft("BatteryUnderV", 30)).append(": ").append(getBatteryUnderV()).append("\n");
		sb.append(InverterUtils.padLeft("BatteryBulkV", 30)).append(": ").append(getBatteryBulkV()).append("\n");
		sb.append(InverterUtils.padLeft("BatteryFloatV", 30)).append(": ").append(getBatteryFloatV()).append("\n");
		sb.append(InverterUtils.padLeft("BatteryType", 30)).append(": ").append(getBatteryType()).append("\n");
		sb.append(InverterUtils.padLeft("MaxGridChargeI", 30)).append(": ").append(getMaxGridChargeI()).append("\n");
		sb.append(InverterUtils.padLeft("MaxChargeI", 30)).append(": ").append(getMaxChargeI()).append("\n");
		sb.append(InverterUtils.padLeft("InputVoltageRange", 30)).append(": ").append(getInputVoltageRange()).append("\n");
		sb.append(InverterUtils.padLeft("OutputSourcePriority", 30)).append(": ").append(getOutputSourcePriority()).append("\n");
		sb.append(InverterUtils.padLeft("ChargerSourcePriority", 30)).append(": ").append(getChargerSourcePriority()).append("\n");
		sb.append(InverterUtils.padLeft("ParallelMax", 30)).append(": ").append(getParallelMax()).append("\n");
		sb.append(InverterUtils.padLeft("MachineType", 30)).append(": ").append(getMachineType()).append("\n");
		sb.append(InverterUtils.padLeft("Topology", 30)).append(": ").append(getTopology()).append("\n");
		sb.append(InverterUtils.padLeft("OutputMode", 30)).append(": ").append(getOutputMode()).append("\n");
		sb.append(InverterUtils.padLeft("BatteryReDischargeV", 30)).append(": ").append(getBatteryReDischargeV()).append("\n");
		sb.append(InverterUtils.padLeft("MaxChargeTimeCV", 30)).append(": ").append(getMaxChargeTimeCV()).append("\n");
		return sb.toString();
	}

	void fromTemplateResult(TemplateResult parts) {
		gridRatingV = parts.getFloat("B");
		gridRatingI = parts.getFloat("C");
		outputRatingV = parts.getFloat("D");
		outputRatingF = parts.getFloat("E");
		outputRatingI = parts.getFloat("F");
		batteryRatingV = parts.getFloat("J");
		batteryRechargeV = parts.getFloat("K");
		batteryUnderV = parts.getFloat("L");
		batteryBulkV = parts.getFloat("M");
		batteryFloatV = parts.getFloat("N");
		batteryReDischargeV = parts.getFloat("V");
		outputRatingApparentP = parts.getInteger("H");
		outputRatingActiveP = parts.getInteger("I");
		maxGridChargeI = parts.getInteger("P");
		maxChargeI = parts.getInteger("Q");
		parallelMax = parts.getInteger("r");
		maxChargeTimeCV = parts.getInteger("Y");

		batteryType = BatteryType.values()[parts.getInteger("O")];
		inputVoltageRange = InputVoltageRange.values()[parts.getInteger("R")];
		chargerSourcePriority = ChargerSourcePriority.values()[parts.getInteger("T")];
		outputSourcePriority = OutputSourcePriority.values()[parts.getInteger("S")];
		machineType = InverterType.values()[parts.getInteger("s")];
		topology = InverterTopology.values()[parts.getInteger("t")];
		outputMode = InverterOutputMode.values()[parts.getInteger("U")];
	}

}
