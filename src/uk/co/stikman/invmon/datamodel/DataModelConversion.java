package uk.co.stikman.invmon.datamodel;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.co.stikman.invmon.inverter.util.InvUtil;

public class DataModelConversion {

	public static void convertV1(Document doc) {
		//
		// fields with a string type used to have a width, now they're a distinct type
		//
		List<Element> all = getAllElements(doc.getDocumentElement(), "Field");
		for (Element el : all) {
			String t = InvUtil.getAttrib(el, "type");
			if (t.equalsIgnoreCase("string")) {
				int w = Integer.parseInt(InvUtil.getAttrib(el, "width"));
				el.removeAttribute("width");
				el.setAttribute("type", "string," + w);
			}
		}
	}

	public static void convertV2(Document doc) {
		//
		// make everything read-only, this should be enough to get it opened
		// enough to be copied into the new format
		//
		for (Element el : getAllElements(doc.getDocumentElement(), "Field")) 
			el.setAttribute("readonly", "true");
	}

	/**
	 * recursively get all elements in this
	 * 
	 * @param root
	 * @param name
	 * @return
	 */
	private static List<Element> getAllElements(Element root, String name) {
		List<Element> lst = new ArrayList<>();
		getAllEls(lst, root, name);
		return lst;
	}

	private static void getAllEls(List<Element> lst, Element root, String name) {
		Node n = root.getFirstChild();
		while (n != null) {
			if (n instanceof Element) {
				Element el = (Element) n;
				if (el.getTagName().equals(name))
					lst.add(el);
				getAllEls(lst, el, name);
			}
			n = n.getNextSibling();
		}
	}

}
