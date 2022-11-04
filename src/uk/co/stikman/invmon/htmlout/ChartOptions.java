package uk.co.stikman.invmon.htmlout;

import java.util.ArrayList;
import java.util.List;

public class ChartOptions {
	private int				pointCount;
	private int				timePeriod;
	private List<Series>	series	= new ArrayList<>();

	public ChartOptions(int pointCount, int timePeriod) {
		super();
		this.pointCount = pointCount;
		this.timePeriod = timePeriod;
	}

	public List<Series> getSeries() {
		return series;
	}

	public int getPointCount() {
		return pointCount;
	}

	public int getTimePeriod() {
		return timePeriod;
	}

	public Series addSeries(String field, List<String> subfields) {
		Series s = new Series(field);
		s.setSubfields(subfields);
		series.add(s);
		return s;
	}

	public Series addSeries(String field) {
		Series s = new Series(field);
		series.add(s);
		return s;
	}

}
