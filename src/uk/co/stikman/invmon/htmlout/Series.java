package uk.co.stikman.invmon.htmlout;

import java.util.Collections;
import java.util.List;

public class Series {

	private String			field;
	private List<String>	subfields	= Collections.emptyList();
	private String			fill		= "transparent";

	public Series(String field) {
		this.field = field;
	}

	public void setSubfields(List<String> subfields) {
		this.subfields = subfields;
	}

	public void setFill(String fill) {
		this.fill = fill;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public List<String> getSubfields() {
		return subfields;
	}

	public String getFill() {
		return fill;
	}

}
