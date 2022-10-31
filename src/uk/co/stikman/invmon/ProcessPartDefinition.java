package uk.co.stikman.invmon;

import org.w3c.dom.Element;

public class ProcessPartDefinition {

	private final String						id;
	private final Element						config;
	private final Class<? extends ProcessPart>	clazz;

	public ProcessPartDefinition(String id, Class<? extends ProcessPart> clazz, Element config) {
		this.id = id;
		this.clazz = clazz;
		this.config = config;
	}

	public Element getConfig() {
		return config;
	}

	public Class<? extends ProcessPart> getClazz() {
		return clazz;
	}

	public String getId() {
		return id;
	}

}
