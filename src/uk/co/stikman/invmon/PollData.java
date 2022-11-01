package uk.co.stikman.invmon;

import java.util.HashMap;
import java.util.Map;

public class PollData {
	private Map<String, InverterDataPoint> data = new HashMap<>();

	public void add(String id, InverterDataPoint rec) {
		data.put(id, rec);
	}

	public <T> T get(String id) {
		return (T) data.get(id);
	}

}
