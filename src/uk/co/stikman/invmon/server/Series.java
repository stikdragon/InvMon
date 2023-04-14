package uk.co.stikman.invmon.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class Series {

	private String			field;
	private List<String>	subfields	= Collections.emptyList();
	private int				yAxis;

	public int getYAxisId() {
		return yAxis;
	}

	/**
	 * if <code>null</code> then it's the first axis
	 * 
	 * @param yAxis
	 */
	public void setYAxis(Axis yAxis) {
		this.yAxis = yAxis.getId();
	}

	public Series(String field) {
		this.field = field;
	}

	public void setSubfields(List<String> subfields) {
		this.subfields = subfields;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public List<String> getSubfields() {
		return subfields;
	}

	public JSONObject toJSON() {
		JSONObject jo = new JSONObject();
		jo.put("field", field);
		JSONArray arr = new JSONArray();
		for (String s : subfields)
			arr.put(s);
		jo.put("subfields", arr);
		jo.put("yAxis", yAxis);
		return jo;
	}

	public void fromJSON(JSONObject root) {
		field = root.getString("field");
		JSONArray arr = root.getJSONArray("subfields");
		subfields = new ArrayList<>();
		for (int i = 0; i < arr.length(); ++i)
			subfields.add(arr.getString(i));
		yAxis = root.getInt("yAxis");
	}

}
