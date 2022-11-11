package uk.co.stikman.invmon.htmlout;

import java.util.Collections;
import java.util.List;

public class Series {

	private String			field;
	private List<String>	subfields	= Collections.emptyList();
	private Axis<?>			yAxis;

	public Axis<?> getYAxis() {
		return yAxis;
	}

	/**
	 * if <code>null</code> then it's the first axis
	 * 
	 * @param yAxis
	 */
	public void setYAxis(Axis<?> yAxis) {
		this.yAxis = yAxis;
	}

	public Series(String field) {
		this.field = field;
	}

	public void setSubfields(List<String> subfields) {
		this.subfields = subfields;
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

}
