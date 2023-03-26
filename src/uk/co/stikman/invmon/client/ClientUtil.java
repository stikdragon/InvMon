package uk.co.stikman.invmon.client;

import java.util.NoSuchElementException;

import org.teavm.jso.browser.Location;

public class ClientUtil {
	/**
	 * return parameter, or <code>def</code> if it's not there. see also
	 * {@link #getURLParam(String)}
	 * 
	 * @param name
	 * @param def
	 * @return
	 */
	public static final String getURLParam(String name, String def) {
		String s = Location.current().getHash();
		if (s == null || s.length() == 0)
			return def;
		s = s.substring(1); // remove the '?'
		String[] bits = s.split("&");
		for (String bit : bits) {
			int n = bit.indexOf('=');
			if (n == -1)
				continue;
			String k = bit.substring(0, n);
			String v = bit.substring(n + 1);
			if (name.equals(k))
				return v;
		}

		return def;
	}

	/**
	 * return parameter, or throw {@link NoSuchElementException} if not present
	 * 
	 * @param name
	 * @return
	 */
	public static final String getURLParam(String name) {
		String s = getURLParam(name, null);
		if (s == null)
			throw new NoSuchElementException("URLParameter [" + name + "] missing");
		return s;
	}

	public static void handleError(Exception e) {
		ErrorPopup pop = InvMon.getErrorPopup();
		pop.addMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
		pop.show();
	}

}
