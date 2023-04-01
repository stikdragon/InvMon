package uk.co.stikman.invmon;

import java.util.HashMap;
import java.util.Map;

public class PollData {
	private Map<String, Sample>	data	= new HashMap<>();
	private long				timestamp;

	public PollData() {
		super();
		timestamp = System.currentTimeMillis();
	}

	public void add(String id, Sample rec) {
		data.put(id, rec);
	}

	public Sample get(String id) {
		return data.get(id);
	}

	public long getTimestamp() {
		return timestamp;
	}

	public Map<String, Sample> getData() {
		return data;
	}

}
