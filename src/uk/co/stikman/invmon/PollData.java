package uk.co.stikman.invmon;

import java.util.HashMap;
import java.util.Map;

public class PollData {
	private Map<String, DataPoint2>	data	= new HashMap<>();
	private long					timestamp;

	public PollData() {
		super();
		timestamp = System.currentTimeMillis();
	}

	public void add(String id, DataPoint2 rec) {
		data.put(id, rec);
	}

	public DataPoint2 get(String id) {
		return data.get(id);
	}

	public long getTimestamp() {
		return timestamp;
	}

}
