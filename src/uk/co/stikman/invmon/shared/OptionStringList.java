package uk.co.stikman.invmon.shared;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class OptionStringList extends Option {

	private List<String> value;

	public OptionStringList(String name) {
		super(name);
	}

	public OptionStringList(String name, String display, List<String> value) {
		super(name, display);
		this.value = value;
	}

	@Override
	public OptionType getType() {
		return OptionType.STRING_LIST;
	}

	public List<String> getValue() {
		return value;
	}

	@Override
	public void toJSON(JSONObject root) {
		super.toJSON(root);
		JSONArray arr = new JSONArray();
		for (String s : value)
			arr.put(s);
		root.put("value", arr);
	}

	@Override
	public void fromJSON(JSONObject root) {
		super.fromJSON(root);
		JSONArray arr = root.getJSONArray("value");
		value = new ArrayList<>();
		for (int i = 0; i < arr.length(); ++i)
			value.add(arr.getString(i));
	}

}
