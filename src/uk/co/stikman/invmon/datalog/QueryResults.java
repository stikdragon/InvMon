package uk.co.stikman.invmon.datalog;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.table.DataRecord;
import uk.co.stikman.table.DataTable;

public class QueryResults {
	private static final Float		ZERO_F	= Float.valueOf(0.0f);
	private static final Integer	ZERO_I	= Integer.valueOf(0);
	private static final Long		ZERO_L	= Long.valueOf(0);

	private List<QueryRecord>		records	= new ArrayList<>();
	private List<Field>				fields	= new ArrayList<>();

	public List<Field> getFields() {
		return fields;
	}

	public List<QueryRecord> getRecords() {
		return records;
	}

	@Override
	public String toString() {
		DataTable dt = new DataTable();
		for (Field f : fields)
			dt.addField(f.getId());

		for (QueryRecord r : records) {
			DataRecord r2 = dt.addRecord();
			for (int i = 0; i < fields.size(); ++i)
				r2.setValue(i, r.asString(i));
		}
		return dt.toString();
	}

	public void addField(Field f) {
		fields.add(f);
	}

	public QueryRecord addRecord() {
		QueryRecord r = new QueryRecord();
		for (Field f : fields) {
			switch (f.getType()) {
				case CURRENT:
				case FLOAT:
				case FREQ:
				case POWER:
				case VOLTAGE:
					r.add(ZERO_F);
					break;
				case STRING:
					r.add("");
					break;
				case INT:
					r.add(ZERO_I);
					break;
				case TIMESTAMP:
					r.add(ZERO_L);
					break;
				default:
					throw new RuntimeException("Unsupported type (not implemented yet): " + f.getType());
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

}
