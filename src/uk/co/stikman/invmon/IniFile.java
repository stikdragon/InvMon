package uk.co.stikman.invmon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import uk.co.stikman.table.DataTable;

public class IniFile {

	public static class Setting {
		private String	key;
		private String	value;

		public Setting(String k, String v) {
			this.key = k;
			this.value = v;
		}

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}
	}

	public static class Section {
		private final String	name;
		private List<Setting>	settings	= new ArrayList<>();

		public Section(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public List<Setting> getSettings() {
			return settings;
		}

		public Setting find(String key) {
			for (Setting s : settings)
				if (key.equals(s.getKey()))
					return s;
			return null;
		}

		public boolean optBoolean(String key, boolean def) {
			Setting s = find(key);
			if (s == null)
				return def;
			return Boolean.parseBoolean(s.value);
		}

		public String optString(String key, String def) {
			Setting s = find(key);
			if (s == null)
				return def;
			return s.value;
		}

	}

	private final Map<String, Section> sections = new HashMap<>();

	public IniFile() {
	}

	public void loadFrom(File f) throws IOException {
		try (InputStream is = new FileInputStream(f)) {
			loadFrom(is);
		}
	}

	public void loadFrom(InputStream is) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		List<String> lines = new ArrayList<>();
		String s;
		while ((s = r.readLine()) != null)
			lines.add(s);

		loadFrom(lines);
	}

	public void loadFrom(List<String> lines) throws IOException {
		Section sect = null;
		for (String line : lines) {
			line = line.trim();
			if (line.isEmpty())
				continue;
			if (line.startsWith("#"))
				continue;
			if (line.startsWith("[")) {
				if (!line.endsWith("]"))
					throw new IOException("Invalid INI file, section name badly formed: " + line);
				String name = line.substring(1, line.length() - 1);
				sect = sections.get(name);
				if (sect == null) {
					sect = new Section(name);
					sections.put(name, sect);
				}
			} else {
				int p = line.indexOf('=');
				if (p == -1)
					throw new IOException("Invalid INI file, key=value pair badly formed: " + line);
				String k = line.substring(0, p);
				String v = line.substring(p + 1);
				sect.settings.add(new Setting(k, v));
			}
		}
	}

	@Override
	public String toString() {
		DataTable dt = new DataTable();
		dt.addFields("Section", "Key", "Value");
		for (Section sect : sections.values().stream().sorted().toList())
			for (Setting s : sect.settings)
				dt.addRecord(sect.getName(), s.key, s.value);
		return dt.toString();
	}

	public Section getSection(String name) {
		Section s = sections.get(name);
		if (s == null)
			throw new NoSuchElementException("Section [" + name + "] does not exist");
		return s;
	}

	public Section findSection(String name) {
		return sections.get(name);
	}
}
