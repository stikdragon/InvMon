package uk.co.stikman.invmon;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.datamodel.FieldVIF;
import uk.co.stikman.invmon.datamodel.VIFReading;
import uk.co.stikman.table.DataTable;

/**
 * a general purpose data point, contains a bunch of values associated with
 * fields in the model
 * 
 * @author stikd
 *
 */
public class DataPoint {
	private final long					timestamp;

	private final Map<Field, Object>	values	= new HashMap<>();

	public DataPoint(long timestamp) {
		super();
		this.timestamp = timestamp;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void put(Field fld, Object val) {
		nonull(fld);
		values.put(fld, val);
	}

	public void put(Field fld, float v) {
		nonull(fld);
		values.put(fld, Float.valueOf(v));
	}

	public void put(Field fld, int v) {
		nonull(fld);
		values.put(fld, Integer.valueOf(v));
	}

	public void put(FieldVIF vif, float v, float i, float f) {
		nonull(vif);
		if (vif.getV() != null)
			values.put(vif.getV(), v);
		if (vif.getI() != null)
			values.put(vif.getI(), i);
		if (vif.getF() != null)
			values.put(vif.getF(), f);
	}

	public float getFloat(Field f) {
		nonull(f);
		Number v = (Number) values.get(f);
		if (v == null)
			return 0.0f;
		return v.floatValue();
	}

	public String getString(Field f) {
		nonull(f);
		Object x = values.get(f);
		if (x == null)
			return null;
		return x.toString();
	}

	public VIFReading getVIF(FieldVIF vif) {
		nonull(vif);
		float v = vif.getV() != null ? getFloat(vif.getV()) : 0.0f;
		float i = vif.getI() != null ? getFloat(vif.getI()) : 0.0f;
		float f = vif.getF() != null ? getFloat(vif.getF()) : 0.0f;
		return new VIFReading(v, i, f);
	}

	private void nonull(Object x) {
		if (x == null)
			throw new NullPointerException();
	}

	public <T extends Enum<T>> T getEnum(Field f, Class<T> cls) {
		return (T) Enum.valueOf(cls, getString(f));
	}

	public Map<Field, Object> getValues() {
		return values;
	}

	@Override
	public String toString() {
		DataTable dt = new DataTable();
		dt.addFields("Field", "Value");
		for (Entry<Field, Object> e : values.entrySet())
			dt.addRecord(e.getKey().getId(), e.getValue().getClass().getSimpleName() + ": " + e.getValue().toString());
		return dt.toString();
	}

}
