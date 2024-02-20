package uk.co.stikman.invmon.datalog;

import uk.co.stikman.invmon.datamodel.FieldType;
import uk.co.stikman.invmon.datamodel.ModelField;

public class QueryFieldModel extends QueryField {

	private ModelField field;

	public QueryFieldModel(ModelField f) {
		this.field = f;
	}

	@Override
	protected String getId() {
		return field.getId();
	}

	@Override
	protected FieldType getType() {
		return field.getType();
	}

}
