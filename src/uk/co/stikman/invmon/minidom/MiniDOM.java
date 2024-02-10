package uk.co.stikman.invmon.minidom;

import java.util.Map.Entry;

import uk.co.stikman.invmon.inverter.util.StringerInputStream;
import uk.co.stikman.invmon.inverter.util.StringerOutputStream;

/**
 * a really basic DOM implementation with just the bare minimum structure to
 * achieve what we need in InvMon. doing this avoids compiling huge amounts of
 * the XML and DOM framework into the target JS
 * 
 * @author stik
 *
 */
public class MiniDOM extends MDElement {

	public MiniDOM() {
		super(null);
	}

	/**
	 * encode this as a string for passing over connections. it's not encoding as
	 * xml! it's also done in a dumb way but it's really quick to implement
	 * 
	 * @return
	 */
	public String encodeAsString() {
		StringerOutputStream sos = new StringerOutputStream();
		encodeOne(sos, this);
		return sos.toString();
	}

	private static void encodeOne(StringerOutputStream sos, MDElement el) {
		sos.writeString(el.getName());
		sos.writeString(el.getTextContent());
		for (Entry<String, String> p : el.getAttribs().entrySet()) {
			sos.writeInt(0);
			sos.writeString(p.getKey());
			sos.writeString(p.getValue());
		}
		for (MDElement x : el) {
			sos.writeInt(1);
			encodeOne(sos, x);
		}
		sos.writeInt(2); // "finished"
	}

	public static MiniDOM decodeString(String src) {
		StringerInputStream sis = new StringerInputStream(src);
		sis.readString(); // name (null for root)
		sis.readString(); // content (also null)
		MiniDOM res = new MiniDOM();
		decodeOne(sis, res);
		return res;
	}

	private static void decodeOne(StringerInputStream sis, MDElement res) {
		for (;;) {
			int n = sis.readInt();
			if (n == 0) {
				String k = sis.readString();
				res.setAttrib(k, sis.readString());
			} else if (n == 1) {
				MDElement el = new MDElement(sis.readString());
				el.setTextContent(sis.readString());
				decodeOne(sis, el);
				res.append(el);
			} else if (n == 2) {
				break;
			} else
				throw new IllegalStateException();
		}
	}
}
