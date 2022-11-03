package uk.co.stikman.invmon.datamodel;

import uk.co.stikman.invmon.datalog.DBRecord;

public interface CalcOp {
	void calc(DBRecord rec, FloatStack stack);
}
