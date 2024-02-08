package uk.co.stikman.invmon.shared;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class OptionEnum extends Option {

	private String			value;
	private List<String>	values;

	public <T extends Enum<T>> OptionEnum(String name, String display, String value, Class<T> eenum) {
		super(name, display);
		this.value = value;
		this.values = new ArrayList<>();
		for (T x : eenum.getEnumConstants())
			values.add(x.name());
	}

	public OptionEnum(String name, String display, String value, List<String> values) {
		super(name, display);
		this.value = value;
		this.values = new ArrayList<>(values);
	}

	public OptionEnum(String name, String display, String value, String csv) {
		super(name, display);
		this.value = value;
		this.values = new ArrayList<>();
		for (String s : csv.split(","))
			values.add(s.trim());
	}

	public OptionEnum(String name) {
		super(name);
	}

	@Override
	public OptionType getType() {
		return OptionType.ENUM;
	}

	public String getValue() {
		return value;
	}

	public List<String> getValues() {
		return values;
	}

	@Override
	public void toJSON(JSONObject root) {
		super.toJSON(root);
		JSONArray arr = new JSONArray();
		for (String s : values)
			arr.put(s);
		root.put("allowed", arr);
		root.put("value", value);
	}

	@Override
	public void fromJSON(JSONObject root) {
		super.fromJSON(root);
		JSONArray arr = root.getJSONArray("allowed");
		values = new ArrayList<>();
		for (int i = 0; i < arr.length(); ++i)
			values.add(arr.getString(i));
		value = root.getString("value");
	}

}
