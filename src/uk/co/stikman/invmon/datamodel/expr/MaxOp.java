package uk.co.stikman.invmon.datamodel.expr;

import uk.co.stikman.invmon.datalog.DBRecord;

public class MaxOp implements CalcOp {

	@Override
	public void calc(DBRecord rec, FloatStack stack) {
		float f1 = stack.pop();
		float f2 = stack.pop();
		stack.push(Math.max(f1, f2));
	}

}
