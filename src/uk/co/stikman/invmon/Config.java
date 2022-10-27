package uk.co.stikman.invmon;

import java.io.File;
import java.io.IOException;

import uk.co.stikman.invmon.IniFile.Section;

public class Config {

	private String	defaultPort		= null;
	private boolean	runWebserver	= false;
	private IniFile	ini;

	public void loadFromFile(File f) throws IOException {
		IniFile ini = new IniFile();
		ini.loadFrom(f);
		this.ini = ini;
		Section sect = ini.getSection("invmon");
		defaultPort = sect.optString("port", null);

		sect = ini.findSection("webserver");
		if (sect != null) {
			runWebserver = sect.optBoolean("enabled", false);
		}

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

}
