package uk.co.stikman.invmon.datamodel.expr;

import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datamodel.ModelField;

public class FetchValOp implements CalcOp {

	private ModelField field;

	public FetchValOp(ModelField field) {
		this.field = field;
	}

	@Override
	public void calc(DBRecord rec, FloatStack stack) {
		stack.push(rec.getFloat(field));
	}

	public ModelField getField() {
		return field;
	}

}
