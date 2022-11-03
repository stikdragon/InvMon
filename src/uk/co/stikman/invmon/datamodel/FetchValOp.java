package uk.co.stikman.invmon.datamodel;

import uk.co.stikman.invmon.datalog.DBRecord;

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
