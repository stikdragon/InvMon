package uk.co.stikman.invmon.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import uk.co.stikman.invmon.datamodel.FieldDataType;
import uk.co.stikman.invmon.htmlout.DataSet;
import uk.co.stikman.invmon.htmlout.DataSetRecord;
import uk.co.stikman.table.DataRecord;
import uk.co.stikman.table.DataTable;

public class DataSetImpl implements DataSet {

	private class Field {
		String			name;
		int				position;
		FieldDataType	type;
	}

	private class Rec implements DataSetRecord {

		private Object[] data;

		public Rec(int size) {
			super();
			data = new Object[size];
		}

		@Override
		public long getLong(int idx) {
			return ((Long) data[idx]).longValue();
		}

		@Override
		public float getFloat(int idx) {
			return ((Number) data[idx]).floatValue();
		}

		@Override
		public int getInt(int idx) {
			return ((Number) data[idx]).intValue();
		}

		@Override
		public String getString(int idx) {
			return (String) data[idx];
		}

	}

	private Map<String, Field>	fields	= new HashMap<>();
	private List<DataSetRecord>	records	= new ArrayList<>();
	private long				start;
	private long				end;

	public void fromJSON(JSONObject root) {
		start = root.getLong("start");
		end = root.getLong("end");
		JSONArray arr = root.getJSONArray("fields");
		for (int i = 0; i < arr.length(); ++i) {
			JSONObject jo = arr.getJSONObject(i);
			Field f = new Field();
			f.name = jo.getString("id");
			char ch = jo.getString("t").charAt(0);
			if (ch == 'f')
				f.type = FieldDataType.FLOAT;
			else if (ch == 'i')
				f.type = FieldDataType.INT;
			else if (ch == 's')
				f.type = FieldDataType.STRING;
			f.position = i;
			fields.put(f.name, f);
		}

		arr = root.getJSONArray("types");
		char[] types = new char[arr.length()];
		for (int i = 0; i < arr.length(); ++i)
			types[i] = arr.getString(i).charAt(0);

		arr = root.getJSONArray("records");
		for (int i = 0; i < arr.length(); ++i) {
			JSONArray arr2 = arr.getJSONArray(i);
			Rec r = new Rec(types.length);

			for (int j = 0; j < types.length; ++j) {
				switch (types[j]) {
					case 'f':
						r.data[j] = arr2.getFloat(j);
						break;
					case 'i':
						r.data[j] = arr2.getInt(j);
						break;
					case 's':
						r.data[j] = arr2.getString(j);
						break;
					case 'l':
						r.data[j] = arr2.getLong(j);
						break;
					default:
						throw new IllegalArgumentException("Unknown field type");
				}
			}
			records.add(r);
		}
	}

	@Override
	public long getStart() {
		return start;
	}

	@Override
	public long getEnd() {
		return end;
	}

	@Override
	public int getFieldIndex(String name) {
		Field f = fields.get(name);
		if (f == null)
			throw new NoSuchElementException(name);
		return f.position;
	}

	@Override
	public List<DataSetRecord> getRecords() {
		return records;
	}

	@Override
	public JSONObject toJSON() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public String toString() {
		DataTable dt = new DataTable();
		List<Field> lst = fields.values().stream().sorted((a, b) -> a.position - b.position).collect(Collectors.toList());
		for (Field f : lst)
			dt.addField(f.name);
		for (DataSetRecord r : records) {
			DataRecord r2 = dt.addRecord();
			int i = 0;
			for (Field f : lst)
				r2.setValue(i++, r.getString(f.position));
		}
		return dt.toString();
	}

}
