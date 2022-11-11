package uk.co.stikman.invmon.htmlout;

import java.util.ArrayList;
import java.util.List;

public class ChartOptions {
	private int				pointCount;
	private Axis<Long>		axisX1	= new Axis<>(0);
	private Axis<Float>		axisY1	= new Axis<>(1);
	private Axis<Float>		axisY2	= new Axis<>(2);
	private List<Series>	series	= new ArrayList<>();
	private long			startTime;
	private long			endTime;

	public ChartOptions(int pointCount, long backwards) {
		this.pointCount = pointCount;
		this.startTime = System.currentTimeMillis() - backwards;
		this.endTime = System.currentTimeMillis();
		axisX1.setEnabled(true);
		axisY1.setEnabled(true);
		axisY2.setEnabled(false); // defaults to off
	}

	public ChartOptions(int pointCount, long tsStart, long tsEnd) {
		super();
		this.pointCount = pointCount;
		this.startTime = tsStart;
		this.endTime = tsEnd;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public Axis<Long> getAxisX1() {
		return axisX1;
	}

	public Axis<Float> getAxisY1() {
		return axisY1;
	}

	public Axis<Float> getAxisY2() {
		return axisY2;
	}

	public List<Series> getSeries() {
		return series;
	}

	public int getPointCount() {
		return pointCount;
	}

	public Series addSeries(String field, List<String> subfields) {
		Series s = new Series(field);
		s.setSubfields(subfields);
		s.setYAxis(axisY1);
		series.add(s);
		return s;
	}

	public Series addSeries(String field) {
		Series s = new Series(field);
		series.add(s);
		s.setYAxis(axisY1);
		return s;
	}

}
