package uk.co.stikman.invmon.minidom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class MDElement implements Iterable<MDElement> {

	private final String		name;
	private Map<String, String>	attribs		= new HashMap<>();
	private List<MDElement>		elements	= new ArrayList<>();
	private MDElement			parent;
	private String				textContent;

	public MDElement(String name) {
		super();
		this.name = name;
	}

	public void append(MDElement child) {
		if (child.parent != null)
			child.parent.remove(child);
		elements.add(child);
		child.parent = this;
	}

	public String getAttrib(String key) {
		if (!attribs.containsKey(key))
			throw new NoSuchElementException(key);
		return attribs.get(key);
	}

	public String getAttrib(String key, String def_) {
		if (attribs.containsKey(key))
			return attribs.get(key);
		return def_;
	}

	public void setAttrib(String key, String val) {
		attribs.put(key, val);
	}

	public Map<String, String> getAttribs() {
		return attribs;
	}

	public List<MDElement> getElements() {
		return elements;
	}

	public MDElement getParent() {
		return parent;
	}

	public String getTextContent() {
		return textContent;
	}

	public boolean hasAttrib(String key) {
		return attribs.containsKey(key);
	}

	public void remove(MDElement child) {
		child.parent = null;
		elements.remove(child);
	}

	public void setTextContent(String textContent) {
		this.textContent = textContent;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public Iterator<MDElement> iterator() {
		return elements.iterator();
	}

	public List<MDElement> getElements(String name) {
		List<MDElement> lst = new ArrayList<>();
		for (MDElement e : this)
			if (name.equals(e.getName()))
				lst.add(e);
		return lst;
	}

	public boolean hasChildren() {
		return !elements.isEmpty();
	}

}
