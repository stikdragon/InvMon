package uk.co.stikman.invmon.htmlout;

import java.util.function.Function;

public class Axis<T extends Number> {
	private float				min;
	private float				max;
	private int					intervals;
	private Function<T, String>	formatter	= f -> f.toString();
	private float				scaleP;

	public float getMin() {
		return min;
	}

	public void setMin(float min) {
		this.min = min;
	}

	public float getMax() {
		return max;
	}

	public void setMax(float max) {
		this.max = max;
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

	public void setScaleP(float f) {
		this.scaleP = f;
	}

	public float getScaleP() {
		return scaleP;
	}

}
