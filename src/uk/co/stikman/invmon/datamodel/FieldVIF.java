package uk.co.stikman.invmon.datamodel;

/**
 * a group of three fields, Voltage, CUrrent, Freq
 * 
 * @author stik
 *
 */
public class FieldVIF {
	private Field	v;
	private Field	i;
	private Field	f;

	public Field getV() {
		return v;
	}

	public void setV(Field v) {
		this.v = v;
	}

	public Field getI() {
		return i;
	}

	public void setI(Field i) {
		this.i = i;
	}

	public Field getF() {
		return f;
	}

	public void setF(Field f) {
		this.f = f;
	}

}
