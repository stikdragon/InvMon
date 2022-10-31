package uk.co.stikman.invmon;

public class VIFReading {
	public static final VIFReading	EMPTY	= new VIFReading(0, 0, 0);
	private final float				v;
	private final float				i;
	private final float				f;

	public VIFReading(float v, float i) {
		this(v, i, 0);
	}

	public VIFReading(float v, float i, float f) {
		super();
		this.v = v;
		this.i = i;
		this.f = f;
	}

	public float getV() {
		return v;
	}

	public float getI() {
		return i;
	}

	public float getF() {
		return f;
	}

	@Override
	public String toString() {
		return "VIFReading [v=" + v + ", i=" + i + ", f=" + f + "]";
	}

	public float getP() {
		return v * i;
	}
}
