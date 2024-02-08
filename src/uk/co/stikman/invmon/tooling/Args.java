package uk.co.stikman.invmon.tooling;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import uk.co.stikman.table.DataTable;

public class Args {

	/**
	 * an Arg can have a null key if it's not a named arg
	 */
	public class Arg {
		private String	key;
		private String	value;

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}

	}

	private List<Arg> args = new ArrayList<>();

	public Args(String[] args) {
		for (int i = 0; i < args.length;) {
			if (args[i].startsWith("-"))
				i = consume(args, i);
			else
				throw new IllegalArgumentException("Invalid command line bit: " + args[i]);
		}
	}

	private int consume(String[] args, int idx) {
		String k = args[idx].substring(1);
		String v = null;
		++idx;
		if (idx < args.length) {
			if (!args[idx].startsWith("-")) {
				v = args[idx];
				++idx;
			}
		}
		Arg a = new Arg();
		a.key = k;
		a.value = v;
		this.args.add(a);
		return idx;
	}

	public List<Arg> getAll() {
		return args;
	}

	public List<String> getAll(String key) {
		return args.stream().filter(x -> key.equals(x.key)).map(x -> x.value).collect(Collectors.toList());
	}

	public String getSingle(String key) {
		List<String> x = getAll(key);
		if (x.isEmpty())
			throw new NoSuchElementException("Command line arg -" + key + " is missing");
		return x.get(0);
	}

	public String findSingle(String key) {
		List<String> x = getAll(key);
		if (x.isEmpty())
			return null;
		return x.get(0);
	}

	public boolean exists(String key) {
		return findSingle(key) != null;
	}

	public int size() {
		return args.size();
	}

	public Arg get(int idx) {
		return args.get(idx);
	}

	@Override
	public String toString() {
		DataTable dt = new DataTable();
		dt.addFields("Key", "Value");
		for (Arg a : args)
			dt.addRecord(a.getKey(), a.getValue() == null ? "" : a.getValue());
		return dt.toString();
	}

	public String getSingle(String key, String default_) {
		String s = findSingle(key);
		if (s == null)
			return default_;
		return s;
	}

}