package uk.co.stikman.invmon.htmlout;

import java.util.ArrayList;
import java.util.List;

public class ChartOptions {
	private Axis<Long>		axisX1	= new Axis<>(0);
	private Axis<Float>		axisY1	= new Axis<>(1);
	private Axis<Float>		axisY2	= new Axis<>(2);
	private List<Series>	series	= new ArrayList<>();
	private int				width	= 800;
	private int				height	= 300;

	public ChartOptions() {
		axisX1.setEnabled(true);
		axisY1.setEnabled(true);
		axisY2.setEnabled(false); // defaults to off
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

	public Series addSeries(String field, List<String> subfields) {
		Series s = new Series(field);
		if (subfields == null)
			subfields = new ArrayList<>();
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

	public void setSize(int w, int h) {
		this.width = w;
		this.height = h;
	}

}
