package uk.co.stikman.invmon.datamodel.expr;

import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datamodel.Field;

public class FetchValOp implements CalcOp {

	private Field field;

	public FetchValOp(Field field) {
		this.field = field;
	}

	@Override
	public void calc(DBRecord rec, FloatStack stack) {
		stack.push(rec.getFloat(field));
	}



}
