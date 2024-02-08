package uk.co.stikman.invmon.shared;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.json.JSONArray;
import org.json.JSONObject;

import uk.co.stikman.invmon.inverter.util.StringerInputStream;
import uk.co.stikman.invmon.inverter.util.StringerOutputStream;

public class WidgetConfigOptions implements Iterable<Option> {
	private List<Option> options = new ArrayList<>();

	public List<Option> getOptions() {
		return options;
	}

	public void toStream(StringerOutputStream sos) {

	}

	public void fromStream(StringerInputStream sis) {

	}

	public void toJSON(JSONObject root) {
		JSONArray arr = new JSONArray();
		root.put("options", arr);
		for (Option opt : options) {
			JSONObject jo = new JSONObject();
			jo.put("class", opt.getClass().getSimpleName());
			jo.put("id", opt.getName());
			opt.toJSON(jo);
			arr.put(jo);
		}
	}

	public void fromJSON(JSONObject root) {
		options = new ArrayList<>();
		JSONArray arr = root.getJSONArray("options");
		for (int i = 0; i < arr.length(); ++i) {
			JSONObject jo = arr.getJSONObject(i);
			//
			// we can't use reflection here since it has to run in JS too
			//
			String cls = jo.getString("class");
			String id = jo.getString("id");
			Option opt = switch (cls) {
				case "OptionString" -> new OptionString(id);
				case "OptionStringList" -> new OptionStringList(id);
				case "OptionFloat" -> new OptionFloat(id);
				case "OptionEnum" -> new OptionEnum(id);
				default -> throw new NoSuchElementException(cls);
			};

			opt.fromJSON(jo);
			options.add(opt);
		}
	}

	public void add(Option opt) {
		if (findOption(opt.getName()) != null)
			throw new IllegalStateException("Option [" + opt.getName() + "] already exists");
		options.add(opt);
	}

	@SuppressWarnings("unchecked")
	private <T extends Option> T findOption(String name) {
		for (Option o : options)
			if (name.equals(o.getName()))
				return (T) o;
		return null;
	}

	public <T extends Option> T get(String name, Class<T> typ) {
		T t = findOption(name);
		if (t == null)
			throw new NoSuchElementException(name);
		return t;
	}

	@Override
	public Iterator<Option> iterator() {
		return options.iterator();
	}
}
