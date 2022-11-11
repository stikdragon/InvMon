package uk.co.stikman.invmon.datamodel.expr;

import uk.co.stikman.invmon.datalog.DBRecord;

public class MulOp implements CalcOp {

	@Override
	public void calc(DBRecord rec, FloatStack stack) {
		float a = stack.pop();
		float b = stack.pop();
		stack.push(a * b);
	}

}
