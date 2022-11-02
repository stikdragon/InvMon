package uk.co.stikman.invmon;

import java.util.HashMap;
import java.util.Map;

import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.inverter.BatteryChargeStage;

/**
 * a general purpose data point, contains a bunch of values associated with
 * fields in the model
 * 
 * @author stikd
 *
 */
public class DataPoint2 {
	private final long					timestamp;

	private final Map<Field, Object>	values	= new HashMap<>();

	public DataPoint2(long timestamp) {
		super();
		this.timestamp = timestamp;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void put(Field fld, Object val) {
		values.put(fld, val);
	}

	public void put(Field fld, float v) {
		values.put(fld, Float.valueOf(v));
	}

	public void put(Field fld, int v) {
		values.put(fld, Integer.valueOf(v));
	}

	public void put(FieldVIF vif, float v, float i, float f) {
		values.put(vif.getV(), v);
		values.put(vif.getI(), i);
		values.put(vif.getF(), f);
	}

	public float getFloat(Field f) {
		Number v = (Number) values.get(f);
		if (v == null)
			return 0.0f;
		return v.floatValue();
	}

	public String getString(Field f) {
		Object x = values.get(f);
		if (x == null)
			return null;
		return x.toString();
	}

	public VIFReading get(FieldVIF vif) {
		return new VIFReading(getFloat(vif.getV()), getFloat(vif.getI()), getFloat(vif.getF()));
	}

	public <T extends Enum<T>> T getEnum(Field f, Class<T> cls) {
		return (T)Enum.valueOf(cls, getString(f));
	}

}
