package uk.co.stikman.invmon.inverter.PIP8048MAX;

import java.util.NoSuchElementException;

public enum OutputMode {
	UTIL_SOL_BAT,
	SOL_BAT_UTIL;

	static OutputMode fromShort(String s) {
		if (s.equalsIgnoreCase("sbu"))
			return SOL_BAT_UTIL;
		if (s.equalsIgnoreCase("usb"))
			return UTIL_SOL_BAT;
		throw new NoSuchElementException("Unknown OutputMode: " + s);
	}
}