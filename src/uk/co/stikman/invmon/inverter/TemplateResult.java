package uk.co.stikman.invmon.inverter;

import java.util.NoSuchElementException;

public interface TemplateResult {
	/**
	 * throws {@link NoSuchElementException} if not present
	 * 
	 * @param key
	 * @return
	 */
	String getString(String key);

	int getInteger(String key);

	float getFloat(String key);

	int getHexInteger(String key);
}
