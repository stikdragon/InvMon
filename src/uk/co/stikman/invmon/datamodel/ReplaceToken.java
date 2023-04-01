package uk.co.stikman.invmon.datamodel;

import java.util.ArrayList;
import java.util.List;

public class ReplaceToken {
	private final String		id;
	private final List<String>	values	= new ArrayList<>();

	public ReplaceToken(String id) {
		super();
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public List<String> getValues() {
		return values;
	}

}
