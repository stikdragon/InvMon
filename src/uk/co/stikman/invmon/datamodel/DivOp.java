package uk.co.stikman.invmon.datamodel;

import uk.co.stikman.invmon.datalog.DBRecord;

public class DivOp implements CalcOp {

	@Override
	public void calc(DBRecord rec, FloatStack stack) {
		float a = stack.pop();
		float b = stack.pop();
		if (b == 0.0f)
			stack.push(0.0f);
		else
			stack.push(a / b);
	}

}
