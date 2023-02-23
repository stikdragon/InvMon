package uk.co.stikman.invmon.inverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import uk.co.stikman.table.DataTable;

public class TemplateResultImpl implements TemplateResult {

	private Map<String, String> map = new HashMap<>();

	public void setPart(String key, String val) {
		map.put(key, val);
	}

	@Override
	public String getString(String key) {
		String x = map.get(key);
		if (x == null)
			throw new NoSuchElementException(key);
		return x;
	}

	@Override
	public int getInteger(String key) {
		String s = getString(key);
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new CommunicationError("Expected integer, found: " + s);
		}
	}

	@Override
	public float getFloat(String key) {
		String s = getString(key);
		try {
			return Float.parseFloat(s);
		} catch (NumberFormatException e) {
			throw new CommunicationError("Expected float, found: " + s);
		}
	}

	@Override
	public int getHexInteger(String key) {
		String s = getString(key);
		try {
			return Integer.parseInt(s, 16);
		} catch (NumberFormatException e) {
			throw new CommunicationError("Expected integer, found: " + s);
		}
	}
	
	@Override
	public String toString() {
		DataTable dt = new DataTable();
		dt.addFields("key", "val");
		map.forEach((k, v) -> dt.addRecord(k, v));
		return dt.toString();
	}

}
