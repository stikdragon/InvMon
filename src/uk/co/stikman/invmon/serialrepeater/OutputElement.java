package uk.co.stikman.invmon.serialrepeater;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.datalog.DBRecord;

public interface OutputElement {

	String eval(Env env, DBRecord rec);

}
