package uk.co.stikman.invmon.server;

/**
 * time units are in seconds
 * 
 * @author stik
 *
 */
public class ChartRenderConfig {
	private int		width		= 700;
	private int		height		= 300;
	private long	offset		= 0;
	private long	duration	= 60;

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

}
