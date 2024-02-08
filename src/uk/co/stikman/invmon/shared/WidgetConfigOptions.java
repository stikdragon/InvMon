package uk.co.stikman.invmon.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import uk.co.stikman.invmon.inverter.util.StringerInputStream;
import uk.co.stikman.invmon.inverter.util.StringerOutputStream;

public class WidgetConfigOptions {
	private List<Option> options = new ArrayList<>();

	public List<Option> getOptions() {
		return options;
	}

	public void toStream(StringerOutputStream sos) {

	}

	public void fromStream(StringerInputStream sis) {

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
}
