package uk.co.stikman.invmon.datalog;

import uk.co.stikman.invmon.datamodel.FieldType;

public abstract class QueryField {

	protected abstract String getId();

	protected abstract FieldType getType();

	@Override
	public String toString() {
		return getId() + " - " + getClass().getSimpleName();
	}

}
