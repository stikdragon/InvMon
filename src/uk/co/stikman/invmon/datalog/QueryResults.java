package uk.co.stikman.invmon.datalog;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.json.JSONArray;
import org.json.JSONObject;

import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.datamodel.FieldType;
import uk.co.stikman.invmon.server.DataSet;
import uk.co.stikman.invmon.server.DataSetRecord;
import uk.co.stikman.table.DataRecord;
import uk.co.stikman.table.DataTable;

public class QueryResults {
	private static final Float		ZERO_F	= Float.valueOf(0.0f);
	private static final Integer	ZERO_I	= Integer.valueOf(0);
	private static final Long		ZERO_L	= Long.valueOf(0);

	private List<QueryRecord>		records	= new ArrayList<>();
	private List<Field>				fields	= new ArrayList<>();

	private long					start;
	private long					end;
	private DataSet					adapter;
	private ZoneId					zone;

	public List<Field> getFields() {
		return fields;
	}

	public List<QueryRecord> getRecords() {
		return records;
	}

	@Override
	public String toString() {
		return toDataTable().toString();
	}

	public DataTable toDataTable() {
		DataTable dt = new DataTable();
		for (Field f : fields)
			dt.addField(f.getId());

		for (QueryRecord r : records) {
			DataRecord r2 = dt.addRecord();
			for (int i = 0; i < fields.size(); ++i)
				r2.setValue(i, r.asString(i));
		}
		return dt;
	}

	public void addField(Field f) {
		fields.add(f);
	}

	public QueryRecord addRecord() {
		QueryRecord r = new QueryRecord(this);
		for (Field f : fields) {
			if (f.getType() == FieldType.TIMESTAMP) {
				r.add(ZERO_L);
			} else {
				switch (f.getType().getBaseType()) {
					case FLOAT:
					case FLOAT8:
						r.add(ZERO_F);
						break;
					case STRING:
						r.add("");
						break;
					case INT:
						r.add(ZERO_I);
						break;
					default:
						throw new RuntimeException("Unsupported type (not implemented yet): " + f.getType());
				}
			}
		}
		records.add(r);
		return r;
	}

	public int getFieldIndex(String name) {
		int i = 0;
		for (Field f : fields) {
			if (name.equals(f.getId()))
				return i;
			++i;
		}
		throw new NoSuchElementException("Field not found: " + name);
	}

	public int findFieldIndex(String name) {
		int i = 0;
		for (Field f : fields) {
			if (name.equals(f.getId()))
				return i;
			++i;
		}
		return -1;
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

	public QueryRecord getLastRecord() {
		return records.get(records.size() - 1);
	}

	public DataSet asDataSet() {
		if (adapter != null)
			return adapter;
		adapter = new DataSet() {
			@Override
			public long getStart() {
				return QueryResults.this.getStart();
			}

			@Override
			public long getEnd() {
				return QueryResults.this.getEnd();
			}

			@Override
			public List<DataSetRecord> getRecords() {
				List<DataSetRecord> lst = new ArrayList<>();
				for (QueryRecord x : records)
					lst.add(x);
				return lst;
			}

			@Override
			public int getFieldIndex(String name) {
				return QueryResults.this.getFieldIndex(name);
			}

			@Override
			public JSONObject toJSON() {
				JSONObject root = new JSONObject();
				root.put("start", getStart());
				root.put("end", getEnd());
				root.put("zone", zone.getId());
				JSONArray arr = new JSONArray();
				root.put("fields", arr);
				for (Field f : fields) {
					JSONObject jo = new JSONObject();
					jo.put("id", f.getId());
					if (f.getType() == FieldType.TIMESTAMP) {
						jo.put("t", "TS");
					} else {
						switch (f.getType().getBaseType()) {
							case FLOAT:
							case FLOAT8:
								jo.put("t", "f");
								break;
							case INT:
								jo.put("t", "i");
								break;
							case STRING:
								jo.put("t", "s");
								break;
							default:
								throw new RuntimeException("Not implemented this");
						}
					}
					arr.put(jo);
				}

				//
				// use first record to work out data types
				//
				char[] types = new char[fields.size()];
				QueryRecord first = records.get(0);
				int i = 0;
				arr = new JSONArray();
				for (Object o : first.getValues()) {
					if (o instanceof Float)
						types[i] = 'f';
					else if (o instanceof Integer)
						types[i] = 'i';
					else if (o instanceof String)
						types[i] = 's';
					else if (o instanceof Long)
						types[i] = 'l';
					arr.put("" + types[i++]);
				}
				root.put("types", arr);

				arr = new JSONArray();
				root.put("records", arr);
				for (QueryRecord r : records) {
					JSONArray arr2 = new JSONArray();
					i = 0;
					for (Object o : r.getValues()) {
						if (types[i] == 'f')
							arr2.put(((Float) o).floatValue());
						else if (types[i] == 'i')
							arr2.put(((Integer) o).intValue());
						else if (types[i] == 's')
							arr2.put((String) o);
						else if (types[i] == 'l')
							arr2.put(((Long) o).toString());
						++i;
					}
					arr.put(arr2);
				}
				return root;
			}

			@Override
			public ZoneId getZone() {
				return zone;
			}
		};
		return adapter;
	}

	public ZoneId getZone() {
		return zone;
	}

	public void setZone(ZoneId zone) {
		this.zone = zone;
	}

}
