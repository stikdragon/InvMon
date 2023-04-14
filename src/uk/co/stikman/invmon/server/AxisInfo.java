package uk.co.stikman.invmon.server;

import org.json.JSONObject;

public class AxisInfo {
	private int		size;
	private float	min;
	private float	max;
	private String	name;

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public float getMin() {
		return min;
	}

	public void setMin(float min) {
		this.min = min;
	}

	public float getMax() {
		return max;
	}

	public void setMax(float max) {
		this.max = max;
	}

	public JSONObject toJSON() {
		JSONObject jo = new JSONObject();
		jo.put("min", min);
		jo.put("max", max);
		jo.put("size", size);
		jo.put("name", name);
		return jo;
	}

	public void fromJSON(JSONObject root) {
		min = root.getFloat("min");
		max = root.getFloat("max");
		size = root.getInt("size");
		name = root.getString("name");
	}

	public float getRange() {
		return max - min;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
