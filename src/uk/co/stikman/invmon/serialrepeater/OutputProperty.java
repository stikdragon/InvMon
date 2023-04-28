package uk.co.stikman.invmon.serialrepeater;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.PropertiesThing;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.inverter.util.InvUtil;

public class OutputProperty implements OutputElement {

	private int		width;
	private String	propId;
	private String	propMod;
	private char	pad	= ' ';

	public OutputProperty(String prop, int width, char pad) {
		this.width = width;
		int n = prop.indexOf('.');
		if (n == -1)
			throw new IllegalArgumentException("<Property> requires a source in the format \"module.id\"");
		this.propMod = prop.substring(0, n);
		this.propId = prop.substring(n + 1);
		this.pad = pad;
	}

	public int getWidth() {
		return width;
	}

	public String getPropId() {
		return propId;
	}

	@Override
	public String eval(Env env, DBRecord rec) {
		PropertiesThing x = env.getModule(propMod);
		String s = x.getProperty(propId, true);
		if (s == null)
			s = "";
		s = InvUtil.padLeft(s, width, pad);
		if (s.length() > width)
			s = s.substring(0, width);
		return s;
	}

}
