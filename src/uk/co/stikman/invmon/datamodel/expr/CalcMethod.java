package uk.co.stikman.invmon.datamodel.expr;

import uk.co.stikman.invmon.datalog.DBRecord;

public interface CalcMethod {
	float calc(DBRecord r);
}
