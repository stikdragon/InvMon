package uk.co.stikman.invmon.datamodel;

/**
 * a group of three fields, Voltage, CUrrent, Freq
 * 
 * @author stik
 *
 */
public class FieldVIF {
	private ModelField	v;
	private ModelField	i;
	private ModelField	f;

	public ModelField getV() {
		return v;
	}

	public void setV(ModelField v) {
		this.v = v;
	}

	public ModelField getI() {
		return i;
	}

	public void setI(ModelField i) {
		this.i = i;
	}

	public ModelField getF() {
		return f;
	}

	public void setF(ModelField f) {
		this.f = f;
	}

}
