package uk.co.stikman.invmon.inverter.PIP8048MAX;

import java.util.HashSet;
import java.util.Set;

import uk.co.stikman.invmon.inverter.InvUtil;

public class DeviceFlags {

	private boolean	buzzer;
	private boolean	overloadBypass;
	private boolean	lcdReturnDefault;
	private boolean	overloadRestart;
	private boolean	overheatRestart;
	private boolean	backlight;
	private boolean	alarmOnPrimaryLoss;
	private boolean	faultRecord;

	public void fromLine(String line) {

		Set<String> enabled = new HashSet<>();

		for (char c : line.toCharArray()) {
			if (c == 'E')
				continue;
			if (c == 'D')
				break;
			enabled.add(Character.toString(c));
		}

		buzzer = enabled.contains("a");
		overloadBypass = enabled.contains("b");
		lcdReturnDefault = enabled.contains("k");
		overloadRestart = enabled.contains("u");
		overheatRestart = enabled.contains("v");
		backlight = enabled.contains("x");
		alarmOnPrimaryLoss = enabled.contains("y");
		faultRecord = enabled.contains("z");
	}

	public boolean isBuzzer() {
		return buzzer;
	}

	public boolean isOverloadBypass() {
		return overloadBypass;
	}

	public boolean isLcdReturnDefault() {
		return lcdReturnDefault;
	}

	public boolean isOverloadRestart() {
		return overloadRestart;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(InvUtil.padLeft("buzzer", 20)).append(": ").append(isBuzzer() ? "TRUE" : "-").append("\n");
		sb.append(InvUtil.padLeft("overloadBypass", 20)).append(": ").append(isOverloadBypass() ? "TRUE" : "-").append("\n");
		sb.append(InvUtil.padLeft("lcdReturnDefault", 20)).append(": ").append(isLcdReturnDefault() ? "TRUE" : "-").append("\n");
		sb.append(InvUtil.padLeft("overloadRestart", 20)).append(": ").append(isOverloadRestart() ? "TRUE" : "-").append("\n");
		sb.append(InvUtil.padLeft("overheatRestart", 20)).append(": ").append(isOverheatRestart() ? "TRUE" : "-").append("\n");
		sb.append(InvUtil.padLeft("backlight", 20)).append(": ").append(isBacklight() ? "TRUE" : "-").append("\n");
		sb.append(InvUtil.padLeft("alarmOnPrimaryLoss", 20)).append(": ").append(isAlarmOnPrimaryLoss() ? "TRUE" : "-").append("\n");
		sb.append(InvUtil.padLeft("faultRecord", 20)).append(": ").append(isFaultRecord() ? "TRUE" : "-").append("\n");
		return sb.toString();
	}

	public boolean isOverheatRestart() {
		return overheatRestart;
	}

	public boolean isBacklight() {
		return backlight;
	}

	public boolean isAlarmOnPrimaryLoss() {
		return alarmOnPrimaryLoss;
	}

	public boolean isFaultRecord() {
		return faultRecord;
	}

}
