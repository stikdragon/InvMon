package uk.co.stikman.invmon.inverter.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import uk.co.stikman.invmon.minidom.MDElement;
import uk.co.stikman.invmon.minidom.MiniDOM;
import uk.co.stikman.invmon.nanohttpd.NanoHTTPD.IHTTPSession;

public class InvUtil {
	public static String padLeft(String s, int len) {
		return padLeft(s, len, ' ');
	}

	public static String padLeft(String s, int len, char pad) {
		if (s == null)
			s = "";
		if (s.length() >= len)
			return s;
		char[] res = new char[len - s.length()];
		for (int i = 0; i < res.length; ++i)
			res[i] = pad;
		return new String(res).concat(s);
	}

	public static String stringOfChar(int n, char ch) {
		if (n <= 0)
			return "";
		char[] arr = new char[n];
		for (int i = 0; i < n; ++i)
			arr[i] = ch;
		return new String(arr);
	}

	public static Document loadXML(File file) throws IOException {
		if (!file.exists())
			throw new FileNotFoundException(file.toString());
		try (FileInputStream fis = new FileInputStream(file)) {
			return loadXML(fis);
		}
	}

	public static void writeMiniDOM(MiniDOM mini, File file) throws IOException {
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

			doc.appendChild(doc.createElement("Page"));
			convertElement(mini, doc.getDocumentElement());

			TransformerFactory xf = TransformerFactory.newInstance();
			Transformer tf = xf.newTransformer();
			tf.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes"); // Set 
			//            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2"); // Set indentation amount
			DOMSource src = new DOMSource(doc);
			StreamResult sr = new StreamResult(file);
			tf.transform(src, sr);
		} catch (Exception e) {
			throw new IOException("Failed to write XML: " + e.getMessage(), e);
		}
	}

	private static void convertElement(MDElement src, Element target) {
		for (Entry<String, String> pair : src.getAttribs().entrySet())
			target.setAttribute(pair.getKey(), pair.getValue());
		for (MDElement x : src) {
			Element y = target.getOwnerDocument().createElement(x.getName());
			convertElement(x, y);
			target.appendChild(y);
		}
	}

	public static MiniDOM loadMiniDOM(File file) throws IOException {
		MiniDOM mini = new MiniDOM();
		Document doc = loadXML(file);
		return miniDomFromReal(doc.getDocumentElement());
	}

	public static MiniDOM miniDomFromReal(Element el) {
		MiniDOM mini = new MiniDOM();
		buildMini(mini, el);
		return mini;
	}

	private static void buildMini(MDElement out, Element el) {
		NamedNodeMap attrs = el.getAttributes();
		if (attrs != null) {
			for (int i = 0; i < attrs.getLength(); i++) {
				Node a = attrs.item(i);
				out.setAttrib(a.getNodeName(), a.getNodeValue());
			}
		}

		Node n = el.getFirstChild();
		while (n != null) {
			if (n instanceof Element e) {
				MDElement el2 = new MDElement(e.getTagName());
				buildMini(el2, e);
				out.append(el2);
			}
			n = n.getNextSibling();
		}
	}

	public static Document loadXML(InputStream fis) throws IOException {
		try {
			DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
			DocumentBuilder b = f.newDocumentBuilder();
			return b.parse(new InputSource(fis));
		} catch (SAXException | IOException | ParserConfigurationException e) {
			throw new IOException("Could not load XML: " + e.getMessage(), e);
		}
	}

	public static Element getElement(Element root, String name, boolean allowmissing) {
		Node n = root.getFirstChild();
		while (n != null) {
			if (n instanceof Element)
				if (((Element) n).getTagName().equals(name))
					return (Element) n;
			n = n.getNextSibling();
		}
		if (allowmissing)
			return null;
		throw new NoSuchElementException("Child element called [" + name + "] not found");
	}

	public static Element getElement(Element root, String name) {
		return getElement(root, name, false);
	}

	public static List<Element> getElements(Element root, String name) {
		List<Element> res = new ArrayList<>();
		Node n = root.getFirstChild();
		while (n != null) {
			if (n instanceof Element && ((Element) n).getTagName().equals(name))
				res.add((Element) n);
			n = n.getNextSibling();
		}
		return res;
	}

	public static List<Element> getElements(Element root) {
		List<Element> res = new ArrayList<>();
		Node n = root.getFirstChild();
		while (n != null) {
			if (n instanceof Element)
				res.add((Element) n);
			n = n.getNextSibling();
		}
		return res;
	}

	public static String getAttrib(Element el, String name) {
		if (!el.hasAttribute(name))
			throw new NoSuchElementException("Element [" + el.getTagName() + "] is missing attribute [" + name + "]");
		return el.getAttribute(name);

	}

	public static String getAttrib(Element el, String name, String def) {
		if (!el.hasAttribute(name))
			return def;
		return el.getAttribute(name);
	}

	public static byte[] readAll(InputStream is) throws IOException {
		int size = 1024;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			byte[] buf = new byte[size];
			int n;
			while ((n = is.read(buf, 0, size)) != -1)
				baos.write(buf, 0, n);
			return baos.toByteArray();
		}
	}

	public static int oom(double f) {
		return (int) Math.log10(f);
	}

	/**
	 * return <code>null</code> if nothing, or the first one if multiple present
	 * 
	 * @param request
	 * @param name
	 * @return
	 */
	public static String getParam(IHTTPSession request, String name) {
		List<String> lst = request.getParameters().get(name);
		if (lst == null || lst.isEmpty())
			return null;
		return lst.get(0);

	}

	public static void sleep(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * return the
	 * 
	 * @param loc
	 * @return
	 */
	public static Map<String, String> getLocationParameters(String s) {
		if (s == null || s.isEmpty())
			return Collections.emptyMap();
		if (s.startsWith("#") || s.startsWith("?")) {
			s = s.substring(1);
			Map<String, String> res = new HashMap<>();
			for (String bit : s.split("&")) {
				String[] x = bit.split("=");
				if (x.length == 2)
					res.put(x[0], x[1]);
			}
			return res;
		}
		return Collections.emptyMap();
	}

	/**
	 * throws exception if there's no "ch" in the string
	 * 
	 * @param s
	 * @param ch
	 * @return
	 */
	public static String[] splitPair(String s, char ch) {
		int p = s.indexOf(ch);
		if (p == -1)
			throw new NoSuchElementException("String [" + s + "] does not contain split char");
		String[] res = new String[2];
		res[0] = s.substring(0, p);
		res[1] = s.substring(p + 1);
		return res;
	}

	private static final String[] SUFFIXES = new String[] { "b", "Kb", "Mb", "Gb", "Tb" };

	public static String formatSize(long n) {
		int i = 0;
		while (n > 1024 * 5) {
			n /= 1024;
			++i;
		}
		return String.format("%d %s", n, SUFFIXES[i]);
	}

	public static float clamp(float val, float min, float max) {
		if (val < min)
			return min;
		if (val > max)
			return max;
		return val;
	}

	public static List<File> getAllFiles(File root, String ext) {
		List<File> lst = new ArrayList<>();
		recurseFiles(root, ext, lst);
		return lst;
	}

	private static void recurseFiles(File root, String ext, List<File> out) {
		if (!root.isDirectory())
			throw new IllegalArgumentException("Root must be directory");
		for (File f : root.listFiles()) {
			if (f.isDirectory()) {
				recurseFiles(f, ext, out);
			} else {
				if (f.toString().endsWith(ext))
					out.add(f);
			}
		}
	}

	public static long parseMilliseconds(String s) {
		long m = 1;
		if (s.endsWith("ms")) {
			s = s.substring(0, s.length() - 2);
		} else if (s.endsWith("s")) {
			m = 1000;
			s = s.substring(0, s.length() - 1);
		} else if (s.endsWith("m")) {
			m = 1000 * 60;
			s = s.substring(0, s.length() - 1);
		} else if (s.endsWith("h")) {
			m = 1000 * 60 * 60;
			s = s.substring(0, s.length() - 1);
		}

		return Long.parseLong(s) * m;
	}

	public static float parsePhysical(PhysicalQuantity dim, String s) {
		try {
			s = s.trim().toLowerCase();

			String unit = null;
			switch (dim) {
				case VOLTAGE:
					unit = "v";
					break;
				case CURRENT:
					unit = "a";
					break;
			}

			if (unit == null)
				throw new UnsupportedOperationException();

			float f = 1.0f;
			if (s.endsWith(unit)) {
				s = s.substring(0, s.length() - 1);

				if (s.endsWith("m")) {
					f = 0.001f;
					s = s.substring(0, s.length() - 1);
				} else if (s.endsWith("u")) {
					f = 0.000001f;
					s = s.substring(0, s.length() - 1);
				}
			}

			return Float.parseFloat(s) * f;
		} catch (Exception e) {
			throw new NumberFormatException("Could not parse [" + s + "] as a " + dim.name() + " because: " + e.getMessage());
		}

	}

	/**
	 * returns <code>null</code> if the regex doesn't match. if it does then returns
	 * the groups as a list of string
	 * 
	 * @param input
	 * @param pattern
	 * @return
	 */
	public static List<String> regex(String input, String regex) {
		Matcher m = Pattern.compile(regex).matcher(input);
		while (m.find()) {
			List<String> lst = new ArrayList<>();
			for (int i = 1; i <= m.groupCount(); i++)
				lst.add(m.group(i));
			return lst;
		}
		return null;
	}


}
