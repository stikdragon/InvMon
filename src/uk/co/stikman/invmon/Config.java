package uk.co.stikman.invmon;

import static uk.co.stikman.invmon.inverter.util.InvUtil.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.controllers.RedSystemController;
import uk.co.stikman.invmon.controllers.StikSystemController;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.inverter.PIP8048MAX.InverterPIPMAX;
import uk.co.stikman.invmon.remote.JSONRecv;
import uk.co.stikman.invmon.remote.JSONSend;
import uk.co.stikman.invmon.serialrepeater.SerialRepeater;
import uk.co.stikman.invmon.server.HTTPServer;

public class Config {

	private interface InvModuleTypeMatcher {
		boolean accepts(Element el);
	}

	private static class SimpleMatcher implements InvModuleTypeMatcher {
		private final String tagName;

		public SimpleMatcher(String tagName) {
			super();
			this.tagName = tagName;
		}

		@Override
		public boolean accepts(Element el) {
			return tagName.equals(el.getTagName());
		}
	}

	private List<InvModDefinition>														things			= new ArrayList<>();
	private int																			updatePeriod;
	private boolean																		allowConversion	= false;
	private File																		modelFile;
	private long																		lastModified;
	private final static List<Pair<InvModuleTypeMatcher, Class<? extends InvModule>>>	thingtypes		= new ArrayList<>();

	static {
		thingtypes.add(new Pair<>(new SimpleMatcher("Inverter"), InverterPIPMAX.class));
		thingtypes.add(new Pair<>(new SimpleMatcher("FakeInverter"), FakeInverterMonitor.class));
		thingtypes.add(new Pair<>(new SimpleMatcher("ConsoleOutput"), ConsoleOutput.class));
		thingtypes.add(new Pair<>(new SimpleMatcher("DataLogger"), DataLogger.class));
		thingtypes.add(new Pair<>(new SimpleMatcher("HTTPServer"), HTTPServer.class));
		thingtypes.add(new Pair<>(new SimpleMatcher("JSONRecv"), JSONRecv.class));
		thingtypes.add(new Pair<>(new SimpleMatcher("JSONSend"), JSONSend.class));
		thingtypes.add(new Pair<>(new SimpleMatcher("SerialRepeater"), SerialRepeater.class));
		thingtypes.add(new Pair<>(new SimpleMatcher("Properties"), PropertiesThing.class));
		thingtypes.add(new Pair<>(el -> el.getTagName().equals("InverterController") && "stik".equals(el.getAttribute("type")), StikSystemController.class));
		thingtypes.add(new Pair<>(el -> el.getTagName().equals("InverterController") && "red".equals(el.getAttribute("type")), RedSystemController.class));
	}

	public void loadFromFile(File f) throws IOException {
		Document doc = loadXML(f);
		Element eset = getElement(doc.getDocumentElement(), "Settings");
		this.updatePeriod = Integer.parseInt(getAttrib(eset, "updatePeriod"));
		this.allowConversion = Boolean.parseBoolean(getAttrib(eset, "allowConversion"));
		this.modelFile = new File(getAttrib(eset, "model"));

		Element emod = getElement(doc.getDocumentElement(), "Modules");
		for (Element el : getElements(emod)) {
			Class<? extends InvModule> cls = null;
			for (Pair<InvModuleTypeMatcher, Class<? extends InvModule>> p : thingtypes) {
				if (p.getA().accepts(el))
					cls = p.getB();
			}
			if (cls == null)
				throw new IllegalArgumentException("No Module could be found for element <" + el.getTagName() + ">");
			String id = getAttrib(el, "id");
			if (findPartDef(id) != null)
				throw new IllegalArgumentException("Object with id [" + id + "] already defined");
			things.add(new InvModDefinition(id, cls, el));
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

	public File getModelFile() {
		return modelFile;
	}

	public long lastFileModified() {
		return lastModified;
	}

}
