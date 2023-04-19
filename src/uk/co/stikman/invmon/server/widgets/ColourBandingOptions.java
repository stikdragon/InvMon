package uk.co.stikman.invmon.server.widgets;

import java.util.ArrayList;
import java.util.List;

public class ColourBandingOptions {
	public class Range {
		private float	v0;
		private float	v1;
		private float	r0;
		private float	g0;
		private float	b0;
		private float	r1;
		private float	g1;
		private float	b1;

		public float getV0() {
			return v0;
		}

		public void setV0(float v0) {
			this.v0 = v0;
		}

		public float getV1() {
			return v1;
		}

		public void setV1(float v1) {
			this.v1 = v1;
		}

		public float getR0() {
			return r0;
		}

		public void setR0(float r0) {
			this.r0 = r0;
		}

		public float getG0() {
			return g0;
		}

		public void setG0(float g0) {
			this.g0 = g0;
		}

		public float getB0() {
			return b0;
		}

		public void setB0(float b0) {
			this.b0 = b0;
		}

		public float getR1() {
			return r1;
		}

		public void setR1(float r1) {
			this.r1 = r1;
		}

		public float getG1() {
			return g1;
		}

		public void setG1(float g1) {
			this.g1 = g1;
		}

		public float getB1() {
			return b1;
		}

		public void setB1(float b1) {
			this.b1 = b1;
		}

		public boolean contains(float v) {
			return v >= v0 && v <= v1;
		}

		public String eval(float v) {
			v -= v0;
			v /= (v1 - v0);
			float u = 1.0f - v;
			float r = u * r0 + v * r1;
			float g = u * g0 + v * g1;
			float b = u * b0 + v * b1;
			return String.format("rgb(%d, %d, %d)", (int) (r * 255), (int) (g * 255), (int) (b * 255));
		}
	}

	private List<Range> ranges = new ArrayList<>();

	public List<Range> getRanges() {
		return ranges;
	}

	public void addBand(float v0, float v1, int c0, int c1) {
		Range range = new Range();
		range.v0 = v0;
		range.v1 = v1;
		range.r0 = (float) ((c0 >> 0) & 0xff) / 255.0f;
		range.g0 = (float) ((c0 >> 8) & 0xff) / 255.0f;
		range.b0 = (float) ((c0 >> 16) & 0xff) / 255.0f;
		range.r1 = (float) ((c1 >> 0) & 0xff) / 255.0f;
		range.g1 = (float) ((c1 >> 8) & 0xff) / 255.0f;
		range.b1 = (float) ((c1 >> 16) & 0xff) / 255.0f;
		ranges.add(range);
	}

	public String eval(float v) {
		for (Range r : ranges)
			if (r.contains(v))
				return r.eval(v);
		return "black";
	}

}
