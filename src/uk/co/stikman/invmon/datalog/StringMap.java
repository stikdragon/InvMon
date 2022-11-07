package uk.co.stikman.invmon.datalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StringMap {
	private List<String>			list	= new ArrayList<>();
	private Map<String, Integer>	lookup	= new HashMap<>();

	public synchronized String get(int idx) {
		return list.get(idx);
	}

	public synchronized int add(String s) {
		Integer idx = lookup.get(s);
		if (idx != null)
			return idx.intValue();
		idx = Integer.valueOf(list.size());
		list.add(s);
		lookup.put(s, idx);
		return idx.intValue();
	}

	/**
	 * return -1 if not present, otherwise index
	 * 
	 * @param s
	 * @return
	 */
	public int contains(String s) {
		Integer i = lookup.get(s);
		if (i == null)
			return -1;
		return i.intValue();
	}
}
