package uk.co.stikman.invmon.client;

import java.util.NoSuchElementException;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Location;

import uk.co.stikman.invmon.client.MessagePopup.Type;

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
		System.err.println(e.getMessage());
		MessagePopup pop = InvMon.getMessagePopup();
		pop.addMessage("Error: " + e.getMessage(), Type.ERROR);
		pop.show();
	}

	public static void handleError(String errmsg) {
		System.err.println(errmsg);
		MessagePopup pop = InvMon.getMessagePopup();
		pop.addMessage("Error: " + errmsg, Type.ERROR);
		pop.show();

	}

	@JSBody(params = { "key", "value" }, script = "localStorage.setItem(key, value);")
	public static native void setLocalStorageItem(String key, String value);

	@JSBody(params = "key", script = "return localStorage.getItem(key);")
	public static native String getLocalStorageItem(String key);

	@JSBody(params = "key", script = "localStorage.removeItem(key);")
	public static native void removeLocalStorageItem(String key);

}
