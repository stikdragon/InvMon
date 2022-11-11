package uk.co.stikman.invmon.htmlout;

import java.util.function.Function;

public class Axis<T extends Number> {

	private final int			id;
	private float				min;
	private float				max;
	private float				minmax;
	private int					intervals;
	private Function<T, String>	formatter	= f -> f.toString();
	private boolean				enabled;
	private Float				forceMin	= null;
	private Float				forceMax	= null;

	public Axis(int id) {
		super();
		this.id = id;
	}

	public float getMin() {
		return min;
	}

	public void setMin(float min) {
		if (forceMin == null)
			this.min = min;
		else
			this.min = forceMin.floatValue();
		minmax = this.max - this.min;
	}

	public float getMax() {
		return max;
	}

	public void setMax(float max) {
		if (forceMax == null)
			this.max = max;
		else
			this.max = forceMax.floatValue();
		minmax = this.max - this.min;
	}

	public int getIntervals() {
		return intervals;
	}

	public void setIntervals(int intervals) {
		this.intervals = intervals;
	}

	public Function<T, String> getFormatter() {
		return formatter;
	}

	public void setFormatter(Function<T, String> formatter) {
		this.formatter = formatter;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * work out where on axis f falls, from 0..1 (can be outside that range if
	 * min/max aren't at the extents of the data)
	 * 
	 * @param f
	 * @return
	 */
	public float eval(float f) {
		if (minmax == 0.0f)
			return 0.0f;
		// i'm not sure about clipping here
		if (f < min)
			return 0.0f;
		if (f > max)
			return 1.0f;
		f = (f - min) / minmax;
		return f;
	}

	public int getId() {
		return id;
	}

	public Float getForceMin() {
		return forceMin;
	}

	public void setForceMin(Float forceMin) {
		this.forceMin = forceMin;
	}

	public Float getForceMax() {
		return forceMax;
	}

	public void setForceMax(Float forceMax) {
		this.forceMax = forceMax;
	}

	public void forceRange(float min, float max) {
		this.forceMin = Float.valueOf(min);
		this.forceMax = Float.valueOf(max);
		this.min = min;
		this.max = max;
		this.minmax = max - min;
	}

	@Override
	public String toString() {
		return "Axis [min=" + min + ", max=" + max + ", enabled=" + enabled + "]";
	}

}
