package uk.co.stikman.invmon.inverter.PIP8048MAX;

import uk.co.stikman.invmon.datamodel.VIFReading;
import uk.co.stikman.invmon.inverter.BatteryChargeStage;
import uk.co.stikman.invmon.inverter.TemplateResult;
import uk.co.stikman.invmon.inverter.util.InvUtil;

public class DeviceStatus {
	private float	gridV;
	private float	gridF;
	private float	gridI;
	private float	outputV;
	private float	outputF;
	private int		outputApparentP;
	private int		outputActiveP;
	private int		outputLoad;
	private int		busV;
	private float	batteryV;
	private int		batteryChargeI;
	private int		batteryCapacity;
	private int		inverterTemp;
	private float	pv1I;
	private float	pv1V;
	private float	sccBatteryV;
	private int		batteryDischargeI;
	private String	deviceStatus;
	private int		fanOffsetV;
	private int		eepromVersion;
	private int		pv1P;
	private String	deviceStatus2;

	private float	pv2V;
	private float	pv2I;
	private float	pv2P;

	public float getGridV() {
		return gridV;
	}

	public float getGridF() {
		return gridF;
	}

	public float getOutputV() {
		return outputV;
	}

	public float getOutputF() {
		return outputF;
	}

	public int getOutputApparentP() {
		return outputApparentP;
	}

	public int getOutputActiveP() {
		return outputActiveP;
	}

	public int getOutputLoad() {
		return outputLoad;
	}

	public int getBusV() {
		return busV;
	}

	public float getBatteryV() {
		return batteryV;
	}

	public int getBatteryChargeI() {
		return batteryChargeI;
	}

	public int getBatteryCapacity() {
		return batteryCapacity;
	}

	public int getInverterTemp() {
		return inverterTemp;
	}

	public float getPv1I() {
		return pv1I;
	}

	public float getPv1V() {
		return pv1V;
	}

	public float getPv2P() {
		return pv2P;
	}

	public float getSccBatteryV() {
		return sccBatteryV;
	}

	public int getBatteryDischargeI() {
		return batteryDischargeI;
	}

	public String getDeviceStatus() {
		return deviceStatus;
	}

	public int getFanOffsetV() {
		return fanOffsetV;
	}

	public int getEepromVersion() {
		return eepromVersion;
	}

	public int getPv1P() {
		return pv1P;
	}

	public String getDeviceStatus2() {
		return deviceStatus2;
	}

	public float getPv2V() {
		return pv2V;
	}

	public float getPv2I() {
		return pv2I;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(InvUtil.padLeft("gridV", 20)).append(": ").append(gridV).append("\n");
		sb.append(InvUtil.padLeft("gridF", 20)).append(": ").append(gridF).append("\n");
		sb.append(InvUtil.padLeft("outputV", 20)).append(": ").append(outputV).append("\n");
		sb.append(InvUtil.padLeft("outputF", 20)).append(": ").append(outputF).append("\n");
		sb.append(InvUtil.padLeft("outputApparentP", 20)).append(": ").append(outputApparentP).append("\n");
		sb.append(InvUtil.padLeft("outputActiveP", 20)).append(": ").append(outputActiveP).append("\n");
		sb.append(InvUtil.padLeft("outputLoad", 20)).append(": ").append(outputLoad).append("\n");
		sb.append(InvUtil.padLeft("busV", 20)).append(": ").append(busV).append("\n");
		sb.append(InvUtil.padLeft("batteryV", 20)).append(": ").append(batteryV).append("\n");
		sb.append(InvUtil.padLeft("batteryChargeI", 20)).append(": ").append(batteryChargeI).append("\n");
		sb.append(InvUtil.padLeft("batteryDischargeI", 20)).append(": ").append(batteryDischargeI).append("\n");
		sb.append(InvUtil.padLeft("batteryCapacity", 20)).append(": ").append(batteryCapacity).append("\n");
		sb.append(InvUtil.padLeft("pv1V", 20)).append(": ").append(pv1V).append("\n");
		sb.append(InvUtil.padLeft("pv1I", 20)).append(": ").append(pv1I).append("\n");
		sb.append(InvUtil.padLeft("pv1P", 20)).append(": ").append(pv1P).append("\n");
		sb.append(InvUtil.padLeft("pv2V", 20)).append(": ").append(pv2V).append("\n");
		sb.append(InvUtil.padLeft("pv2I", 20)).append(": ").append(pv2I).append("\n");
		sb.append(InvUtil.padLeft("pv2P", 20)).append(": ").append(pv2P).append("\n");
		sb.append(InvUtil.padLeft("inverterTemp", 20)).append(": ").append(inverterTemp).append("\n");
		sb.append(InvUtil.padLeft("sccBatteryV", 20)).append(": ").append(sccBatteryV).append("\n");
		sb.append(InvUtil.padLeft("deviceStatus", 20)).append(": ").append(deviceStatus).append("\n");
		sb.append(InvUtil.padLeft("deviceStatus2", 20)).append(": ").append(deviceStatus2).append("\n");
		sb.append(InvUtil.padLeft("fanOffsetV", 20)).append(": ").append(fanOffsetV).append("\n");
		sb.append(InvUtil.padLeft("eepromVersion", 20)).append(": ").append(eepromVersion).append("\n");

		return sb.toString();
	}

	public void fromTemplateResultPart1(TemplateResult parts) {
		outputApparentP = parts.getInteger("F");
		outputActiveP = parts.getInteger("G");
		outputLoad = parts.getInteger("H");
		busV = parts.getInteger("I");
		batteryChargeI = parts.getInteger("K");
		batteryCapacity = parts.getInteger("O");
		inverterTemp = parts.getInteger("T");
		batteryDischargeI = parts.getInteger("P");
		fanOffsetV = parts.getInteger("Q");
		eepromVersion = parts.getInteger("V");
		gridV = parts.getFloat("B");
		gridF = parts.getFloat("C");
		outputV = parts.getFloat("D");
		outputF = parts.getFloat("E");
		batteryV = parts.getFloat("J");
		pv1P = parts.getInteger("M");
		pv1I = parts.getFloat("e");
		pv1V = parts.getFloat("U");
		sccBatteryV = parts.getFloat("W");
		deviceStatus = parts.getString("A");
		deviceStatus2 = parts.getString("Z");
	}

	public void fromTemplateResultPart2(TemplateResult parts) {
		pv2I = parts.getFloat("A");
		pv2V = parts.getFloat("B");
		pv2P = parts.getFloat("C");
	}

	public BatteryChargeStage getBatteryChargeStage() {
		if (batteryChargeI == 0 && batteryDischargeI == 0)
			return BatteryChargeStage.IDLE;
		if (batteryDischargeI > 0)
			return BatteryChargeStage.DISCHARGING;
		if (deviceStatus.charAt(4) == '1')
			return BatteryChargeStage.CHARGE_ABSORB;
		if (deviceStatus2.charAt(0) == '1')
			return BatteryChargeStage.CHARGE_FLOAT;
		return BatteryChargeStage.CHARGE_BULK; // i guess if it's not the others then it must be this?
	}

	public float getGridI() {
		return gridI;
	}

	public void setGridI(float gridI) {
		this.gridI = gridI;
	}

}
