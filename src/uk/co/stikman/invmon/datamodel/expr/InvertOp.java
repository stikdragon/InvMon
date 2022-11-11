package uk.co.stikman.invmon.datamodel.expr;

import uk.co.stikman.invmon.datalog.DBRecord;

public class InvertOp implements CalcOp {

	@Override
	public void calc(DBRecord rec, FloatStack stack) {
		stack.push(-stack.pop());
	}

}
