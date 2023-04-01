package uk.co.stikman.invmon;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import uk.co.stikman.invmon.datamodel.FieldVIF;
import uk.co.stikman.invmon.datamodel.VIFReading;
import uk.co.stikman.table.DataTable;

/**
 * simple String:value list
 * 
 * @author stik
 *
 */
public class Sample {
	private Map<String, Object>	values	= new HashMap<>();
	private long				timestamp;

	public Sample(long timestamp) {
		super();
		this.timestamp = timestamp;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void put(String fld, Object val) {
		nonull(fld);
		values.put(fld, val);
	}

	public void put(String fld, float v) {
		nonull(fld);
		values.put(fld, Float.valueOf(v));
	}

	public void put(String fld, int v) {
		nonull(fld);
		values.put(fld, Integer.valueOf(v));
	}

	/**
	 * appends V, I, and F to the field name
	 * 
	 * @param vif
	 * @param v
	 * @param i
	 * @param f
	 */
	public void put(String vif, float v, float i, float f) {
		nonull(vif);
		values.put(vif + "V", v);
		values.put(vif + "I", i);
		values.put(vif + "F", f);
	}

	public VIFReading getVIF(String fld) {
		VIFReading x = new VIFReading(getFloat(fld + "V"), getFloat(fld + "I"), getFloat(fld + "F"));
		return x;
	}

	public void put(String fields, VIFReading vif) {
		put(fields, vif.getV(), vif.getI(), vif.getF());
	}

	public float getFloat(String f) {
		nonull(f);
		Number v = (Number) values.get(f);
		if (v == null)
			return 0.0f;
		return v.floatValue();
	}

	public int getInt(String f) {
		nonull(f);
		Number v = (Number) values.get(f);
		if (v == null)
			return 0;
		return v.intValue();
	}

	public String getString(String f) {
		nonull(f);
		Object x = values.get(f);
		if (x == null)
			return null;
		return x.toString();
	}

	private void nonull(Object x) {
		if (x == null)
			throw new NullPointerException();
	}

	public Map<String, Object> getValues() {
		return values;
	}

	@Override
	public String toString() {
		DataTable dt = new DataTable();
		dt.addFields("Field", "Value");
		for (Entry<String, Object> e : values.entrySet())
			dt.addRecord(e.getKey(), e.getValue().getClass().getSimpleName() + ": " + e.getValue().toString());
		return dt.toString();
	}

}
