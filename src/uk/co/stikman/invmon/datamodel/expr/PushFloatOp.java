package uk.co.stikman.invmon.datamodel.expr;

import uk.co.stikman.invmon.datalog.DBRecord;

public class PushFloatOp implements CalcOp {
	private final float val;

	public PushFloatOp(float val) {
		super();
		this.val = val;
	}

	@Override
	public void calc(DBRecord rec, FloatStack stack) {
		stack.push(val);
	}

}
