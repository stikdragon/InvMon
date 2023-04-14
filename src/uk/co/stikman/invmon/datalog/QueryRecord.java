package uk.co.stikman.invmon.datalog;

import java.util.ArrayList;
import java.util.List;

import uk.co.stikman.invmon.datamodel.VIFReading;
import uk.co.stikman.invmon.server.DataSetRecord;

public class QueryRecord implements DataSetRecord {

	private final QueryResults	owner;
	private List<Object>		values	= new ArrayList<>();
	private int					baseRecordCount;

	public QueryRecord(QueryResults owner) {
		super();
		this.owner = owner;
	}

	@Override
	public float getFloat(int idx) {
		return ((Float) values.get(idx)).floatValue();
	}

	@Override
	public long getLong(int idx) {
		return ((Long) values.get(idx)).longValue();
	}

	@Override
	public int getInt(int idx) {
		return ((Integer) values.get(idx)).intValue();
	}

	@Override
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

	public Number getNumber(String field) {
		Object o = values.get(owner.getFieldIndex(field));
		if (o instanceof Number)
			return (Number) o;
		throw new ClassCastException("Field [" + field + "] is not a Number");
	}

	public List<Object> getValues() {
		return values;
	}

}
