package uk.co.stikman.invmon;

import org.w3c.dom.Element;

public class InvModDefinition {

	private final String						id;
	private final Element						config;
	private final Class<? extends InvModule>	clazz;

	public InvModDefinition(String id, Class<? extends InvModule> clazz, Element config) {
		this.id = id;
		this.clazz = clazz;
		this.config = config;
	}

	public Element getConfig() {
		return config;
	}

	public Class<? extends InvModule> getClazz() {
		return clazz;
	}

	public String getId() {
		return id;
	}

}
