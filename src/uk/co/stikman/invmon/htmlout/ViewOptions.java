package uk.co.stikman.invmon.htmlout;

public class ViewOptions {
	private int		duration	= 60;
	private long	offset		= 0;

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}
}
