package uk.co.stikman.invmon.datalog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.DataPoint;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.datamodel.AggregationMode;
import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.inverter.InvUtil;

public class DataLogger extends InvModule {

	private MiniDB	db;
	private File	lock;
	private File	file;
	private int		blockSize;

	public DataLogger(String id, Env env) {
		super(id, env);
	}

	public void start() throws InvMonException {
		super.start();
		db = new MiniDB(file, blockSize);
		db.setModel(getEnv().getModel());
		try {
			db.open();
			System.out.println(db.toDataTable());
		} catch (MiniDbException e) {
			throw new InvMonException("Failed to start database: " + e.getMessage(), e);
		}
	}

	@Subscribe(Events.POST_DATA)
	public void postData(PollData data) throws MiniDbException {
		//
		// insert record into database
		//
		Field fts = db.getModel().get("TIMESTAMP");
		DBRecord rec = db.addRecord();
		rec.setLong(fts, data.getTimestamp());
		for (DataPoint x : data.getData().values()) {
			for (Entry<Field, Object> e : x.getValues().entrySet()) {
				switch (e.getKey().getType()) {
				case FLOAT:
				case FREQ:
				case CURRENT:
				case POWER:
				case VOLTAGE:
					rec.setFloat(e.getKey(), ((Number) e.getValue()).floatValue());
					break;
				case STRING:
					rec.setString(e.getKey(), e.getValue().toString());
					break;
				case INT:
					rec.setInt(e.getKey(), ((Number) e.getValue()).intValue());
					break;
				default:
					throw new IllegalStateException("unsupported field type: " + e.getKey().getType());
				}
			}
		}
		db.commitRecord(rec);
	}

	@Override
	public void configure(Element config) {
		file = new File(InvUtil.getAttrib(config, "file"));
		lock = new File(file.getAbsolutePath() + ".lock");
		blockSize = Integer.parseInt(InvUtil.getAttrib(config, "blockSize", Integer.toString(MiniDB.DEFAULT_BLOCKSIZE)));
	}

	@Override
	public void terminate() {
		super.terminate();
	}


	public QueryResults query(long tsStart, long tsEnd, int points, List<String> fieldnames) throws MiniDbException {
		DataModel model = getEnv().getModel();
		if (!fieldnames.get(0).equals("TIMESTAMP"))
			fieldnames.add(0, "TIMESTAMP");
		List<Field> fields = fieldnames.stream().map(model::get).collect(Collectors.toList());
		Field fts = fields.get(0); // timestamp field
		long span = tsEnd - tsStart;

		//
		// make records for the results
		//
		QueryResults res = new QueryResults();
		for (Field f : fields)
			res.addField(f);
		List<QueryRecord> recs = new ArrayList<>();
		for (int i = 0; i < points; ++i) {
			QueryRecord r = res.addRecord();
			r.setLong(0, tsStart + i * span / points);
			recs.add(r);
		}

		//
		// find all records in the source data between these times
		//
		IntRange range = db.getRecordRange(fts, tsStart, tsEnd);
		if (range.isValid()) {
			for (int idx = range.getLow(); idx < range.getHigh(); ++idx) {
				DBRecord dbrec = db.getRecord(idx);
				long ts = dbrec.getLong(fts);
				int slot = (int) (points * (ts - tsStart) / span);
				QueryRecord outrec = recs.get(slot);
				for (int i = 1; i < fields.size(); ++i) { // starts at 1! (skip key field)
					Field srcfld = fields.get(i);

					switch (srcfld.getType()) {
					case CURRENT:
					case FLOAT:
					case FREQ:
					case VOLTAGE:
					case POWER:
						switch (srcfld.getAggregationMode()) {
						case FIRST:
							if (outrec.getBaseRecordCount() == 0)
								outrec.setFloat(i, dbrec.getFloat(srcfld));
							break;
						case MAX:
							outrec.setFloat(i, Math.max(dbrec.getFloat(srcfld), outrec.getFloat(i)));
							break;
						case MIN:
							outrec.setFloat(i, Math.min(dbrec.getFloat(srcfld), outrec.getFloat(i)));
							break;
						case MEAN:
						case SUM:
							outrec.setFloat(i, dbrec.getFloat(srcfld) + outrec.getFloat(i));
							break;
						}
						break;
					case STRING:
						switch (srcfld.getAggregationMode()) {
						case FIRST:
							if (outrec.getBaseRecordCount() == 0)
								outrec.setString(i, dbrec.getString(srcfld));
							break;
						case MAX:
							outrec.setString(i, maxString(dbrec.getString(srcfld), outrec.getString(i)));
							break;
						case MIN:
							outrec.setString(i, minString(dbrec.getString(srcfld), outrec.getString(i)));
							break;
						default:
							throw new MiniDbException("Invalid aggregation mode for string type: " + srcfld.getAggregationMode());
						}
						break;
					case TIMESTAMP:
						switch (srcfld.getAggregationMode()) {
						case FIRST:
							if (outrec.getBaseRecordCount() == 0)
								outrec.setLong(i, dbrec.getLong(srcfld));
							break;
						case MAX:
							outrec.setLong(i, Math.max(dbrec.getLong(srcfld), outrec.getLong(i)));
							break;
						case MIN:
							outrec.setLong(i, Math.min(dbrec.getLong(srcfld), outrec.getLong(i)));
							break;
						case MEAN:
						case SUM:
							outrec.setLong(i, dbrec.getLong(srcfld) + outrec.getLong(i));
							break;
						}
						break;
					case INT:
						switch (srcfld.getAggregationMode()) {
						case FIRST:
							if (outrec.getBaseRecordCount() == 0)
								outrec.setInt(i, dbrec.getInt(srcfld));
							break;
						case MAX:
							outrec.setInt(i, Math.max(dbrec.getInt(srcfld), outrec.getInt(i)));
							break;
						case MIN:
							outrec.setInt(i, Math.min(dbrec.getInt(srcfld), outrec.getInt(i)));
							break;
						case MEAN:
						case SUM:
							outrec.setInt(i, dbrec.getInt(srcfld) + outrec.getInt(i));
							break;
						}
						break;
					default:
						throw new MiniDbException("Unsupported type: " + srcfld.getType());
					}
				}
				outrec.setBaseRecordCount(outrec.getBaseRecordCount() + 1);
			}
		}

		//
		// deal with averages
		//
		for (QueryRecord r : res.getRecords()) {
			for (int i = 1; i < fields.size(); ++i) {
				Field f = fields.get(i);
				if (f.getAggregationMode() == AggregationMode.MEAN)
					r.setFloat(i, r.getBaseRecordCount() == 0 ? 0.0f : r.getFloat(i) / r.getBaseRecordCount());
			}
		}

		return res;
	}

	private static String maxString(String a, String b) {
		if (a.compareTo(b) > 0)
			return a;
		return b;
	}

	private static String minString(String a, String b) {
		if (a.compareTo(b) < 0)
			return a;
		return b;
	}

}
