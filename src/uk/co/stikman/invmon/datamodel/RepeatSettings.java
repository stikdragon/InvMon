package uk.co.stikman.invmon.datamodel;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import uk.co.stikman.table.DataTable;

public class RepeatSettings {
	private Map<String, Integer> counts = new HashMap<>();

	public int getCountForGroup(String name) {
		if (counts.containsKey(name))
			return ((Integer) counts.get(name)).intValue();
		throw new NoSuchElementException("Repeat Group [" + name + "] has not been assigned");
	}
	public int getCountForGroup(String name, int def) {
		if (counts.containsKey(name))
			return ((Integer) counts.get(name)).intValue();
		return def;
	}

	public void setCountForGroup(String name, int n) {
		counts.put(name, Integer.valueOf(n));
	}

	@Override
	public String toString() {
		DataTable dt = new DataTable();
		dt.addFields("Group", "Count");
		counts.keySet().stream().sorted().forEach(k -> dt.addRecord(k, counts.get(k).toString()));
		return dt.toString();
	}
}
