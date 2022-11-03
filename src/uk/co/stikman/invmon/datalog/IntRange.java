package uk.co.stikman.invmon.datalog;

public class IntRange {

	private int	low;
	private int	high;

	public IntRange(int low, int high) {
		super();
		this.low = low;
		this.high = high;
	}

	public int getLow() {
		return low;
	}

	public void setLow(int low) {
		this.low = low;
	}

	public int getHigh() {
		return high;
	}

	public void setHigh(int high) {
		this.high = high;
	}

	public boolean isValid() {
		return low > -1 && high >= -1;
	}

	@Override
	public String toString() {
		return "[" + low + " - " + high + "]";
	}

}
