package uk.co.stikman.invmon.serialrepeater;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datamodel.ModelField;

public class OutputField implements OutputElement {
	private ModelField	f;
	private int		width;
	private float	scale;

	public OutputField(ModelField f, int width, float scale) {
		super();
		this.f = f;
		this.width = width;
		this.scale = scale;
	}

	public ModelField getF() {
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
