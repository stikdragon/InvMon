package uk.co.stikman.invmon;

import static uk.co.stikman.invmon.inverter.InverterUtils.getAttrib;
import static uk.co.stikman.invmon.inverter.InverterUtils.getElements;
import static uk.co.stikman.invmon.inverter.InverterUtils.loadXML;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.inverter.PIP8048MAX.InverterMonitorPIP;

public class Config {

	private String						defaultPort		= null;
	private boolean						runWebserver	= false;
	private IniFile						ini;
	private List<ProcessPartDefinition>	things			= new ArrayList<>();

	public void loadFromFile(File f) throws IOException {
		Document doc = loadXML(f);
		Map<String, Class<? extends ProcessPart>> thingtypes = new HashMap<>();
		thingtypes.put("Inverter", InverterMonitorPIP.class);
		thingtypes.put("FakeInverter", FakeInverterMonitor.class);
		thingtypes.put("ConsoleOutput", ConsoleOutput.class);
		thingtypes.put("HTMLOutput", HTMLOutput.class);

		for (Element el : getElements(doc.getDocumentElement())) {
			Class<? extends ProcessPart> cls = thingtypes.get(el.getTagName());
			if (cls != null) {
				String id = getAttrib(el, "id");
				if (findPartDef(id) != null)
					throw new IllegalArgumentException("Object with id [" + id + "] already defined");
				things.add(new ProcessPartDefinition(id, cls, el));
			}
		}
	}

	public ProcessPartDefinition findPartDef(String id) {
		return things.stream().filter(x -> x.getId().equals(id)).findAny().orElse(null);
	}

	public boolean isRunWebserver() {
		return runWebserver;
	}

	public String getDefaultPort() {
		return defaultPort;
	}

	public IniFile getIni() {
		return ini;
	}

	public List<ProcessPartDefinition> getThings() {
		return things;
	}

}
