package uk.co.stikman.invmon.datalog;

public class BlockInfo {
	private int		id;
	private int		startIndex;
	private int		endIndex;
	private long	startTS;
	private long	endTS;

	@Override
	public String toString() {
		return "BlockInfo [id=" + id + ", startIndex=" + startIndex + ", endIndex=" + endIndex + ", startTS=" + startTS + ", endTS=" + endTS + "]";
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}

	public long getStartTS() {
		return startTS;
	}

	public void setStartTS(long startTS) {
		this.startTS = startTS;
	}

	public long getEndTS() {
		return endTS;
	}

	public void setEndTS(long endTS) {
		this.endTS = endTS;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

}
