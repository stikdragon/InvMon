package uk.co.stikman.invmon.server;

import java.time.ZoneId;
import java.util.function.Function;

import org.json.JSONObject;

import uk.co.stikman.invmon.inverter.util.Format;

public class Axis {

	private int									id;
	private float								min;
	private float								max;
	private transient float						minmax;
	private int									intervals;
	private transient Function<Number, String>	formatter	= f -> f.toString();
	private boolean								enabled;
	private Float								forceMin	= null;
	private Float								forceMax	= null;
	private String								format;
	private int									size		= 50;
	private String								name;

	public Axis(int id) {
		super();
		this.id = id;
	}

	public JSONObject toJSON() {
		JSONObject jo = new JSONObject();
		jo.put("id", id);
		jo.put("min", min);
		jo.put("max", max);
		jo.put("intervals", intervals);
		jo.put("enabled", enabled);
		jo.put("forceMin", forceMin == null ? null : forceMin.floatValue());
		jo.put("forceMax", forceMax == null ? null : forceMax.floatValue());
		jo.put("format", format);
		jo.put("size", size);
		jo.put("name", name);
		return jo;
	}

	public void fromJSON(JSONObject root) {
		id = root.getInt("id");
		min = root.getFloat("min");
		max = root.getFloat("max");
		intervals = root.getInt("intervals");
		enabled = root.getBoolean("enabled");
		forceMax = root.has("forceMax") ? Float.valueOf(root.getFloat("forceMax")) : null;
		forceMin = root.has("forceMin") ? Float.valueOf(root.getFloat("forceMin")) : null;
		setFormat(root.optString("format", null));
		size = root.getInt("size");
		name = root.getString("name");
	}

	public float getMin() {
		return min;
	}

	public void setMin(float min) {
		if (forceMin == null)
			this.min = min;
		else
			this.min = forceMin.floatValue();
		minmax = this.max - this.min;
	}

	public float getMax() {
		return max;
	}

	public void setMax(float max) {
		if (forceMax == null)
			this.max = max;
		else
			this.max = forceMax.floatValue();
		minmax = this.max - this.min;
	}

	public int getIntervals() {
		return intervals;
	}

	public void setIntervals(int intervals) {
		this.intervals = intervals;
	}

	public Function<Number, String> getFormatter() {
		return formatter;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * work out where on axis f falls, from 0..1 (can be outside that range if
	 * min/max aren't at the extents of the data)
	 * 
	 * @param f
	 * @return
	 */
	public float eval(float f) {
		if (minmax == 0.0f)
			return 0.0f;
		// i'm not sure about clipping here
		if (f < min)
			return 0.0f;
		if (f > max)
			return 1.0f;
		f = (f - min) / minmax;
		return f;
	}

	public int getId() {
		return id;
	}

	public Float getForceMin() {
		return forceMin;
	}

	public void setForceMin(Float forceMin) {
		this.forceMin = forceMin;
	}

	public Float getForceMax() {
		return forceMax;
	}

	public void setForceMax(Float forceMax) {
		this.forceMax = forceMax;
	}

	public void forceRange(float min, float max) {
		this.forceMin = Float.valueOf(min);
		this.forceMax = Float.valueOf(max);
		this.min = min;
		this.max = max;
		this.minmax = max - min;
	}

	@Override
	public String toString() {
		return "Axis [min=" + min + ", max=" + max + ", enabled=" + enabled + "]";
	}

	public void setFormat(String s, ZoneId zone) {
		this.format = s;
		if (s == null) {
			formatter = f -> f.toString();
		} else {
			final Format f = new Format(s);
			f.setTimezone(zone);
			formatter = n -> f.format(n);
		}
	}
	
	public void setFormat(String s) {
		setFormat(s, ZoneId.systemDefault());
	}

	public String getFormat() {
		return format;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * return 0 if disabled
	 * 
	 * @param x
	 * @return
	 */
	public int axisSize() {
		return isEnabled() ? getSize() : 0;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}



}
