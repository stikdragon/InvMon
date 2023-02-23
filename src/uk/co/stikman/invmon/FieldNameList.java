package uk.co.stikman.invmon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class FieldNameList implements Iterable<String> {
	private Set<String> list = new HashSet<>();

	/**
	 * can do CSV here
	 * 
	 * @param fields
	 */
	public void add(String fields) {
		for (String s : fields.split(","))
			list.add(s.trim());
	}

	public int size() {
		return list.size();
	}

	@Override
	public Iterator<String> iterator() {
		return list.iterator();
	}

	public List<String> asList() {
		List<String> lst = new ArrayList<>();
		list.forEach(lst::add);
		return lst;
	}

}
