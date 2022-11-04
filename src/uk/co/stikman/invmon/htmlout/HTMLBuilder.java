package uk.co.stikman.invmon.htmlout;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import uk.co.stikman.invmon.inverter.InvUtil;

public class HTMLBuilder {
	private StringBuilder sb = new StringBuilder();

	@Override
	public String toString() {
		return sb.toString();
	}

	public HTMLBuilder append(Class<?> cls, String resourceName) {
		try (InputStream is = cls.getResourceAsStream(resourceName)) {
			sb.append(new String(InvUtil.readAll(is), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new RuntimeException("Error loading resource[" + resourceName + "]: " + e.getMessage(), e);
		}
		return this;
	}

	public HTMLBuilder append(String s) {
		sb.append(s);
		return this;
	}

	public HTMLBuilder div(String cls) {
		append("<div class=\"");
		append(cls);
		append("\">");
		return this;
	}

	public HTMLBuilder append(float f) {
		append(Float.toString(f));
		return this;
	}

}
