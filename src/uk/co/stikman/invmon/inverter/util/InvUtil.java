package uk.co.stikman.invmon.inverter.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class InvUtil {
	public static String padLeft(String s, int len) {
		if (s == null)
			s = "";
		if (s.length() >= len)
			return s;
		char[] res = new char[len - s.length()];
		for (int i = 0; i < res.length; ++i)
			res[i] = ' ';
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
		try (FileInputStream fis = new FileInputStream(file)) {
			return loadXML(fis);
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

	public static Element getElement(Element root, String name) {
		Node n = root.getFirstChild();
		while (n != null) {
			if (n instanceof Element)
				if (((Element) n).getTagName().equals(name))
					return (Element) n;
			n = n.getNextSibling();
		}
		throw new NoSuchElementException("Child element called [" + name + "] not found");
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

}
