package uk.co.stikman.invmon.htmlout;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.json.JSONArray;
import org.json.JSONObject;

public class ChartOptions {
	private Axis<Long>		axisX1	= new Axis<>(0);
	private Axis<Float>		axisY1	= new Axis<>(1);
	private Axis<Float>		axisY2	= new Axis<>(2);
	private List<Series>	series	= new ArrayList<>();
	private int				width	= 800;
	private int				height	= 300;

	public ChartOptions() {
		setAxisDefault(axisX1, true, 22, "Time");
		setAxisDefault(axisY1, true, 55, "Y");
		setAxisDefault(axisY2, false, 5, "Y2"); // defaults to off 
	}

	private void setAxisDefault(Axis<?> ax, boolean enabled, int size, String name) {
		ax.setEnabled(enabled);
		ax.setSize(size);
		ax.setName(name);
	}

	public void fromJSON(JSONObject root) {
		width = root.getInt("width");
		height = root.getInt("height");
		JSONArray arr = root.getJSONArray("series");
		for (int i = 0; i < arr.length(); ++i) {
			Series s = new Series(null);
			s.fromJSON(arr.getJSONObject(i));
			series.add(s);
		}

		axisX1 = new Axis<>(0);
		axisY1 = new Axis<>(0);
		axisY2 = new Axis<>(0);
		axisX1.fromJSON(root.getJSONObject("axisX1"));
		axisY1.fromJSON(root.getJSONObject("axisY1"));
		axisY2.fromJSON(root.getJSONObject("axisY2"));
	}

	public JSONObject toJSON() {
		JSONObject root = new JSONObject();
		root.put("axisX1", axisX1.toJSON());
		root.put("axisY1", axisY1.toJSON());
		root.put("axisY2", axisY2.toJSON());
		JSONArray arr = new JSONArray();
		for (Series s : series)
			arr.put(s.toJSON());
		root.put("series", arr);
		root.put("width", width);
		root.put("height", height);

		return root;
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

	public Axis<?> getAxis(int id) {
		if (axisX1.getId() == id)
			return axisX1;
		if (axisY1.getId() == id)
			return axisY1;
		if (axisY2.getId() == id)
			return axisY2;
		throw new NoSuchElementException("Axis " + id + " not known");
	}

}
