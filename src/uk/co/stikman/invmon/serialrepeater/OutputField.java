package uk.co.stikman.invmon.serialrepeater;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datamodel.Field;

public class OutputField implements OutputElement {
	private Field	f;
	private int		width;
	private float	scale;

	public OutputField(Field f, int width, float scale) {
		super();
		this.f = f;
		this.width = width;
		this.scale = scale;
	}

	public Field getF() {
		return f;
	}

	public int getWidth() {
		return width;
	}

	public float getScale() {
		return scale;
	}

	@Override
	public String eval(Env env, DBRecord rec) {
		float a = rec.getFloat(f);
		int n = (int) (a * scale);
		return String.format("%0" + width + "d", n);
	}

}
