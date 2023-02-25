package uk.co.stikman.invmon.datamodel;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.co.stikman.invmon.inverter.InvUtil;

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
