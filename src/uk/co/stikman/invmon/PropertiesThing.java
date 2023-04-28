package uk.co.stikman.invmon;

import static uk.co.stikman.invmon.inverter.util.InvUtil.loadXML;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.log.StikLog;

public class PropertiesThing extends InvModule {
	private static final StikLog	LOGGER		= StikLog.getLogger(PropertiesThing.class);
	private File					file;
	private Map<String, String>		properties	= new HashMap<>();
	private long					lastUpdate;

	public PropertiesThing(String id, Env env) {
		super(id, env);
	}

	@Override
	public void configure(Element root) throws InvMonException {
		String s = InvUtil.getAttrib(root, "source", null);
		if (s != null) {
			this.file = new File(s);
			if (!file.exists())
				throw new InvMonException("Source file for <Properties> element does not exist: " + s);
		} else {
			file = null;
		}

		propsFromDom(root);
		try {
			loadFromFile();
		} catch (IOException e) {
			throw new InvMonException(e);
		}
	}

	private void propsFromDom(Element root) {
		for (Element el : InvUtil.getElements(root, "Property"))
			properties.put(InvUtil.getAttrib(el, "id"), el.getTextContent());
	}

	private void loadFromFile() throws IOException {
		if (file == null)
			return;
		File f = getEnv().getFile(file.toString());
		Document doc = loadXML(f);
		propsFromDom(doc.getDocumentElement());
		lastUpdate = file.lastModified();
	}

	@Subscribe(Events.TIMER_UPDATE_PERIOD)
	public void postData() {
		if (file != null) {
			//
			// check the source file for updates
			//
			if (file.lastModified() != lastUpdate) {
				try {
					loadFromFile();
				} catch (Exception e) {
					LOGGER.warn("Could not reload <Properties> file because: " + e.getMessage(), e);
				}
			}
		}
	}

	public String getProperty(String key) {
		return getProperty(key, true);
	}

	public String getProperty(String key, boolean allowMissing) {
		if (properties.containsKey(key)) {
			return properties.get(key);
		} else {
			if (!allowMissing)
				throw new NoSuchElementException("Property [" + key + "] is not defined");
			return null;
		}
	}

}
