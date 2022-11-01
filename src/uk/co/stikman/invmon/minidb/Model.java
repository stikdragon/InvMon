package uk.co.stikman.invmon.minidb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.w3c.dom.Document;

import uk.co.stikman.invmon.inverter.InverterUtils;

public class Model {
	private Map<String, ModelField> fields = new HashMap<>();

	public void loadXML(InputStream str) throws IOException {
		Document doc = InverterUtils.loadXML(str);
	}

	public void writeXML(OutputStream str) {
		throw new RuntimeException("Not implemented yet");
	}

	public ModelField get(String name) {
		ModelField x = find(name);
		if (x == null)
			throw new NoSuchElementException("Field [" + name + "] does not exist");
		return x;
	}

	public ModelField find(String name) {
		return fields.get(name);
	}

	public void add(ModelField fld) {
		if (find(fld.getName()) != null)
			throw new IllegalStateException("Field [" + fld.getName() + "] already exists");
		fields.put(fld.getName(), fld);
	}

}
