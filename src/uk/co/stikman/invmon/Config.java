package uk.co.stikman.invmon;

import static uk.co.stikman.invmon.inverter.util.InvUtil.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.inverter.PIP8048MAX.InverterPIPMAX;
import uk.co.stikman.invmon.remote.JSONRecv;
import uk.co.stikman.invmon.remote.JSONSend;
import uk.co.stikman.invmon.serialrepeater.SerialRepeater;
import uk.co.stikman.invmon.server.HTTPServer;

public class Config {

	private List<InvModDefinition>									things			= new ArrayList<>();
	private int														updatePeriod;
	private boolean													allowConversion	= false;
	private File													modelFile;
	private long													lastModified;
	private final static Map<String, Class<? extends InvModule>>	thingtypes		= new HashMap<>();

	static {
		thingtypes.put("Inverter", InverterPIPMAX.class);
		thingtypes.put("FakeInverter", FakeInverterMonitor.class);
		thingtypes.put("ConsoleOutput", ConsoleOutput.class);
		thingtypes.put("DataLogger", DataLogger.class);
		thingtypes.put("HTTPServer", HTTPServer.class);
		thingtypes.put("JSONRecv", JSONRecv.class);
		thingtypes.put("JSONSend", JSONSend.class);
		thingtypes.put("SerialRepeater", SerialRepeater.class);
		thingtypes.put("Properties", PropertiesThing.class);
	}

	public void loadFromFile(File f) throws IOException {
		Document doc = loadXML(f);
		Element eset = getElement(doc.getDocumentElement(), "Settings");
		this.updatePeriod = Integer.parseInt(getAttrib(eset, "updatePeriod"));
		this.allowConversion = Boolean.parseBoolean(getAttrib(eset, "allowConversion"));
		this.modelFile = new File(getAttrib(eset, "model"));

		Element emod = getElement(doc.getDocumentElement(), "Modules");
		for (Element el : getElements(emod)) {
			Class<? extends InvModule> cls = thingtypes.get(el.getTagName());
			if (cls != null) {
				String id = getAttrib(el, "id");
				if (findPartDef(id) != null)
					throw new IllegalArgumentException("Object with id [" + id + "] already defined");
				things.add(new InvModDefinition(id, cls, el));
			}
		}
		this.lastModified = f.lastModified();
	}

	public InvModDefinition findPartDef(String id) {
		return things.stream().filter(x -> x.getId().equals(id)).findAny().orElse(null);
	}

	public List<InvModDefinition> getThings() {
		return things;
	}

	public int getUpdatePeriod() {
		return updatePeriod;
	}

	public boolean isAllowConversion() {
		return allowConversion;
	}

	public static Map<String, Class<? extends InvModule>> getThingTypes() {
		return thingtypes;
	}

	public File getModelFile() {
		return modelFile;
	}

	public long lastFileModified() {
		return lastModified;
	}

}
