package uk.co.stikman.invmon.server;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.inverter.util.Format;

public class HeaderBitPF extends HeaderBitDef {

	private static final String f = "PF: [%1.2f] (Real Power: [%d]W @ [%1.2f]A)";

	@Override
	public String execute(Env env, DBRecord rec) {
		DataModel mdl = env.getModel();
		float real = rec.getFloat(mdl.get("LOAD_P"));
		float pf = rec.getFloat(mdl.get("LOAD_PF"));

		Format fmt = new Format(f);
		String s = fmt.format(pf, real * pf, rec.getFloat(mdl.get("LOAD_I")));
		return boldSquareBits(s);
	}

}
