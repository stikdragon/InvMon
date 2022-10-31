package uk.co.stikman.invmon.inverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

public class InverterUtils {
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
			DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
			DocumentBuilder b = f.newDocumentBuilder();
			return b.parse(new InputSource(fis));
		} catch (SAXException | IOException | ParserConfigurationException e) {
			throw new IOException("Could not load XML: " + e.getMessage(), e);
		}
	}

	public static List<Element> getElements(Element doc) {
		List<Element> res = new ArrayList<>();
		Node n = doc.getFirstChild();
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

}
