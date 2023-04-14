package uk.co.stikman.invmon.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import uk.co.stikman.invmon.inverter.util.Format;
import uk.co.stikman.invmon.inverter.util.InvUtil;

public class HTMLBuilder {
	private StringBuilder sb = new StringBuilder();

	public HTMLBuilder() {
	}

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

	public static String readResource(Class<?> cls, String resourceName) {
		try (InputStream is = cls.getResourceAsStream(resourceName)) {
			return new String(InvUtil.readAll(is), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("Error loading resource[" + resourceName + "]: " + e.getMessage(), e);
		}
	}

	public HTMLBuilder append(String s) {
		sb.append(s);
		return this;
	}

	public HTMLBuilder escape(String s) {
		sb.append(escapeHTML(s));
		return this;
	}

	private static String escapeHTML(String s) {
		StringBuilder out = new StringBuilder(Math.max(16, s.length()));
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c > 127 || c == '"' || c == '\'' || c == '<' || c == '>' || c == '&') {
				out.append("&#");
				out.append((int) c);
				out.append(';');
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}

	public HTMLBuilder append(String fmt, Object... args) {
		Format f = new Format(fmt);
		sb.append(f.format(args));
		//		sb.append(String.format(fmt, args));
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

	public HTMLBuilder append(int n) {
		append(Integer.toString(n));
		return this;
	}

	public void clear() {
		sb = new StringBuilder();
	}

}
