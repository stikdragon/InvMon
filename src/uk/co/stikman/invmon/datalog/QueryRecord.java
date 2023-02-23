package uk.co.stikman.invmon.datalog;

import java.util.ArrayList;
import java.util.List;

import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.datamodel.FieldVIF;
import uk.co.stikman.invmon.datamodel.VIFReading;

public class QueryRecord {

	private final QueryResults	owner;
	private List<Object>		values	= new ArrayList<>();
	private int					baseRecordCount;

	public QueryRecord(QueryResults owner) {
		super();
		this.owner = owner;
	}

	public float getFloat(int idx) {
		return ((Float) values.get(idx)).floatValue();
	}

	public long getLong(int idx) {
		return ((Long) values.get(idx)).longValue();
	}

	public int getInt(int idx) {
		return ((Integer) values.get(idx)).intValue();
	}

	public String getString(int idx) {
		return (String) values.get(idx);
	}

	public void add(Object o) {
		values.add(o);
	}

	public void setLong(int idx, long val) {
		values.set(idx, Long.valueOf(val));
	}

	public void setFloat(int idx, float val) {
		values.set(idx, Float.valueOf(val));
	}

	public void setInt(int idx, int val) {
		values.set(idx, Integer.valueOf(val));
	}

	public int getBaseRecordCount() {
		return baseRecordCount;
	}

	public void setBaseRecordCount(int baseRecordCount) {
		this.baseRecordCount = baseRecordCount;
	}

	public void setString(int idx, String s) {
		values.set(idx, s);
	}

	public String asString(int idx) {
		Object v = values.get(idx);
		if (v == null)
			return null;
		return v.toString();
	}

	public VIFReading getVIF(String name) {
		int i_v = owner.findFieldIndex(name + "_V");
		int i_i = owner.findFieldIndex(name + "_I");
		int i_f = owner.findFieldIndex(name + "_F");
		return new VIFReading(i_v == -1 ? 0.0f : getFloat(i_v), i_i == -1 ? 0.0f : getFloat(i_i), i_f == -1 ? 0.0f : getFloat(i_f));
	}

	public float getFloat(String name) {
		return getFloat(owner.getFieldIndex(name));
	}

	public int getInt(String field) {
		return getInt(owner.getFieldIndex(field));
	}

}
