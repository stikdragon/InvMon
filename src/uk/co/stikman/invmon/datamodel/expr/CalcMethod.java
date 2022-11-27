package uk.co.stikman.invmon.datamodel.expr;

import java.util.List;

import uk.co.stikman.invmon.datalog.DBRecord;

public interface CalcMethod {
	float calc(DBRecord r);
	List<CalcOp> getOps();
}
