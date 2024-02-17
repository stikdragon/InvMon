package uk.co.stikman.invmon.datalog;

import java.io.File;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.Sample;
import uk.co.stikman.invmon.datamodel.AggregationMode;
import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.datamodel.FieldType;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.log.StikLog;

public class DataLogger extends InvModule {
	private static final StikLog	LOGGER					= StikLog.getLogger(DataLogger.class);
	private MiniDB					db;
	private StatsDB					statsDb;
	private File					statsDbFile;
	private File					lock;
	private File					file;
	private int						blockSize;
	private int						cachedBlocks;
	private Set<String>				suppressedErrorMessages	= new HashSet<>();
	private long					pauseStatsUntil			= -1;

	public DataLogger(String id, Env env) {
		super(id, env);
	}

	public void start() throws InvMonException {
		super.start();
		LOGGER.info("Loading database from [" + file.toString() + "]...");

		//
		// make sure directory exists
		//
		if (!file.getParentFile().exists())
			throw new RuntimeException("Data Directory [" + file.getParentFile().getAbsolutePath() + "] does not exist");

		db = new MiniDB(file, blockSize);
		db.setModel(getEnv().getModel());
		try {
			db.open();
		} catch (MiniDbException e) {
			throw new InvMonException("Failed to start database: " + e.getMessage(), e);
		} catch (ModelChangeException e) {
			try {
				doConversion(getEnv().getModel());
				System.gc();
			} catch (MiniDbException e1) {
				throw new InvMonException(e1);
			}

			//
			// now attempt to reopen it
			//
			db = new MiniDB(file, blockSize);
			db.setModel(getEnv().getModel());
			try {
				db.open();
			} catch (Exception ee) {
				LOGGER.error("Error converting database.  The original files should still exist with the extension .old");
				throw new InvMonException("Attempted to convert database, but failed to open it because: " + ee.getMessage(), ee);
			}
		}
		LOGGER.info("MiniDB heap block size: " + InvUtil.formatSize(db.getModel().getRecordHeapSize() * db.getBlockSize()));
		LOGGER.info("MiniDB max cached blocks: " + cachedBlocks);
		db.setMaxCachedBlocks(cachedBlocks);

		LOGGER.info("Opening StatsDB..");
		statsDb = new StatsDB(this, statsDbFile);

		LOGGER.info("  done.");
	}

	private void doConversion(DataModel model) throws MiniDbException {
		LOGGER.info("Model has changed, attempting to convert database...");
		if (!getEnv().getConfig().isAllowConversion())
			throw new MiniDbException("Database needs conversion, but [allowConversion] is not set to [true] in the <Settings> element of the config file");
		LOGGER.info("  (This may take a long time)");

		File fold = new File(file.toString() + ".old");
		if (fold.exists()) {
			LOGGER.error("");
			LOGGER.error("=====================================");
			LOGGER.error("");
			LOGGER.error("We will rename the old database to " + fold.getName() + " but there is already a file");
			LOGGER.error("by that name.  This probably left over from a prior upgrade process.  You must ");
			LOGGER.error("delete these *.old files first.");
			LOGGER.error("");
			LOGGER.error("=====================================");
			LOGGER.error("");
			throw new MiniDbException("*.old database already exists");
		}

		//
		// make a new DB with a temp name, convert into it, if it's
		// ok then delete the old one and rename
		//
		MiniDB newDb = new MiniDB(new File(file.toString() + ".tmpconv"), blockSize);
		MiniDB oldDb = new MiniDB(file, blockSize);
		try {
			try {
				oldDb.open();
				newDb.setModel(model);
				newDb.open();

				//
				// make a load of mappings from old to new
				//
				Set<String> oldFieldNames = new HashSet<>();
				Set<String> newFieldNames = new HashSet<>();
				for (Field f : oldDb.getModel())
					oldFieldNames.add(f.getId());
				for (Field f : newDb.getModel())
					newFieldNames.add(f.getId());

				Set<String> xsect = new HashSet<>(oldFieldNames);
				xsect.retainAll(newFieldNames);

				Map<String, String> copyFields = new HashMap<>();
				xsect.forEach(s -> copyFields.put(s, s));

				//
				// now we'll do some copying of specific fields between database versions
				//
				if (oldDb.getModel().getDataVersion() == 1 && newDb.getModel().getDataVersion() == 2) {
					for (int i = 1; i <= 8; ++i)
						if (newDb.getModel().find("BATT_I_" + i) != null)
							copyFields.put("BATT_I_" + i, "INV_" + i + "_I");
				}

				long lastT = 0;
				for (int i = 0; i < oldDb.getRecordCount(); ++i) {
					if (System.currentTimeMillis() - lastT > 1500) {
						lastT = System.currentTimeMillis();
						LOGGER.info(String.format("%.1f%% Converting record [%d] of [%d]", (float) (100 * i) / oldDb.getRecordCount(), i, oldDb.getRecordCount()));
						//						LOGGER.info("Open blocks: " + oldDb.getOpenBlocks()  + ",  " + newDb.getOpenBlocks());
					}

					DBRecord oldR = oldDb.getRecord(i);
					DBRecord newR = newDb.addRecord();
					newR.setTimestamp(oldR.getTimestamp());

					for (Entry<String, String> e : copyFields.entrySet()) {
						Field newF = newDb.getModel().get(e.getValue());
						Field oldF = oldDb.getModel().get(e.getKey());

						if (!newF.getType().equals(oldF.getType())) // TODO: i suppose we could convert these?
							throw new MiniDbException("Cannot convert database as field [" + newF.getId() + "] has changed data type");

						if (newF.getCalculationMethod() == null) { // can't set calculated fields
							if (newF.getType() == FieldType.TIMESTAMP) // skip this 
								continue;
							switch (newF.getType().getBaseType()) {
								case FLOAT:
									newR.setFloat(newF, oldR.getFloat(oldF));
									break;
								case INT:
									newR.setInt(newF, oldR.getInt(oldF));
									break;
								case STRING:
									newR.setString(newF, oldR.getString(oldF));
									break;
								case FLOAT8:
									newR.setFloat8(newF, oldR.getFloat8(oldF));
									break;
							}
						}
					}

					newDb.commitRecord(newR);
				}
			} finally {
				oldDb.close();
				newDb.close();
			}

			//
			// now we can attempt to rename them
			//
			oldDb.rename(new File(file.toString() + ".old"));
			newDb.rename(file);

			LOGGER.warn("");
			LOGGER.warn("****************************************************************************");
			LOGGER.warn("");
			LOGGER.warn("Existing database has been upgraded to the latest format, everything appears");
			LOGGER.info("to have worked, but just in case the original data has been left behind with");
			LOGGER.info("the name [" + (new File(file.toString() + ".old").toString()) + "]");
			LOGGER.info("If you're happy that everything is working correctly then you should delete");
			LOGGER.info("these files.");
			LOGGER.warn("");
			LOGGER.warn("****************************************************************************");
			LOGGER.warn("");

		} catch (Exception e) {
			throw new MiniDbException("Failed to convert database: " + e.getMessage(), e);
		}

	}

	@Subscribe(Events.POST_DATA)
	public void postData(PollData data) throws MiniDbException {
		//
		// insert record into database
		//
		DBRecord last = db.getLastRecord();
		if (last != null) {
			if (data.getTimestamp() <= last.getTimestamp()) {
				LOGGER.warn("Failed to append record. Records must be in timestamp order (this can be caused by the system clock going backwards)");
				LOGGER.warn("  Last record: " + last.toString());
				LOGGER.warn("  PollData:    " + data);
				return;
			}
		}
		DBRecord rec = db.addRecord();
		rec.setTimestamp(data.getTimestamp());

		//
		// work through the Source fields for all fields and fetch their values
		//
		for (Field f : getEnv().getModel()) {
			if (f.getSource() != null) {
				String[] bits = InvUtil.splitPair(f.getSource(), '.');
				Sample src = data.get(bits[0]);

				if (src == null) {
					//
					// show an error the first time, then ignore in future
					//
					String s = "Source [" + f.getSource() + ":" + bits[1] + "] was not found in the samples returned by active modules";
					if (!suppressedErrorMessages.contains(s)) {
						LOGGER.error(s);
						suppressedErrorMessages.add(s);
					}
					continue;
				}
				switch (f.getType().getBaseType()) {
					case FLOAT:
						rec.setFloat(f, src.getFloat(bits[1]));
						break;
					case STRING:
						rec.setString(f, src.getString(bits[1]));
						break;
					case INT:
						rec.setInt(f, src.getInt(bits[1]));
						break;
					case FLOAT8:
						rec.setFloat8(f, src.getFloat(bits[1]));
						break;
					default:
						throw new IllegalStateException("unsupported field type: " + f.getType());
				}
			}
		}

		db.commitRecord(rec);

		processStats(rec);

		getEnv().getBus().fire(Events.LOGGER_RECORD_COMMITED, rec);
	}

	/**
	 * a record has been committed, also maintain the stats database
	 * 
	 * @param rec
	 * @throws InvMonException
	 */
	private void processStats(DBRecord rec) {
		if (pauseStatsUntil > -1 && pauseStatsUntil < System.currentTimeMillis())
			return;
		pauseStatsUntil = -1;
		try {
			statsDb.update(rec);
		} catch (InvMonException e) {
			pauseStatsUntil = System.currentTimeMillis();
			LOGGER.error("Stats update failed, stopping for 1 hour: " + e.getMessage(), e);
		}
	}

	@Override
	public void configure(Element config) throws InvMonException {
		file = new File(InvUtil.getAttrib(config, "file"));
		statsDbFile = new File(InvUtil.getAttrib(config, "stats"));
		lock = new File(file.getAbsolutePath() + ".lock");
		blockSize = Integer.parseInt(InvUtil.getAttrib(config, "blockSize", Integer.toString(MiniDB.DEFAULT_BLOCKSIZE)));
		cachedBlocks = Integer.parseInt(InvUtil.getAttrib(config, "cachedBlocks", Integer.toString(MiniDB.DEFAULT_CACHED_BLOCKS)));

		Element el = InvUtil.getElement(config, "StatsConfig", true);
		if (el != null) {
			statsDb = new StatsDB(this, statsDbFile);
			statsDb.configure(el);
		}
	}

	@Override
	public void terminate() {
		super.terminate();
	}

	public QueryResults query(long tsStart, long tsEnd, int points, List<String> fieldnames) throws MiniDbException {
		if (tsStart > tsEnd)
			throw new MiniDbException("Range is smaller than 0");
		DataModel model = getEnv().getModel();
		if (!fieldnames.get(0).equals("TIMESTAMP"))
			fieldnames.add(0, "TIMESTAMP");
		List<Field> fields = fieldnames.stream().map(model::get).collect(Collectors.toList());
		long span = tsEnd - tsStart;

		//
		// make records for the results
		//
		QueryResults res = new QueryResults();
		res.setZone(ZoneId.systemDefault());
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
		IntRange range = db.getRecordRange(tsStart, tsEnd);
		if (range.isValid()) {
			for (int idx = range.getLow(); idx < range.getHigh(); ++idx) {
				DBRecord dbrec = db.getRecord(idx);
				long ts = dbrec.getTimestamp();
				int slot = (int) (points * (ts - tsStart) / span);
				QueryRecord outrec = recs.get(slot);
				for (int i = 1; i < fields.size(); ++i) { // starts at 1! (skip key field)
					Field srcfld = fields.get(i);

					if (srcfld.getType() == FieldType.TIMESTAMP) {
						switch (srcfld.getAggregationMode()) {
							case FIRST:
								if (outrec.getBaseRecordCount() == 0)
									outrec.setLong(i, dbrec.getTimestamp());
								break;
							case MAX:
								outrec.setLong(i, Math.max(dbrec.getTimestamp(), outrec.getLong(i)));
								break;
							case MIN:
								outrec.setLong(i, Math.min(dbrec.getTimestamp(), outrec.getLong(i)));
								break;
							case MEAN:
							case SUM:
								outrec.setLong(i, dbrec.getTimestamp() + outrec.getLong(i));
								break;
						}
					} else {

						switch (srcfld.getType().getBaseType()) {
							case FLOAT:
							case FLOAT8:
								float f = dbrec.getFloat(srcfld);
								if (!Float.isFinite(f))
									f = 0.0f;
								switch (srcfld.getAggregationMode()) {
									case FIRST:
										if (outrec.getBaseRecordCount() == 0)
											outrec.setFloat(i, f);
										break;
									case MAX:
										outrec.setFloat(i, Math.max(f, outrec.getFloat(i)));
										break;
									case MIN:
										outrec.setFloat(i, Math.min(f, outrec.getFloat(i)));
										break;
									case MEAN:
									case SUM:
										outrec.setFloat(i, f + outrec.getFloat(i));
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

		res.setStart(tsStart);
		res.setEnd(tsEnd);

		return res;
	}

	public void forEachRecordIn(long tsStart, long tsEnd, Consumer<DBRecord> callback) throws MiniDbException {
		if (tsStart > tsEnd)
			throw new MiniDbException("Range is smaller than 0");
		IntRange range = db.getRecordRange(tsStart, tsEnd);
		if (range.isValid()) {
			for (int idx = range.getLow(); idx < range.getHigh(); ++idx)
				callback.accept(db.getRecord(idx));
		}
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

	public int getLastRecordId() throws MiniDbException {
		return db.getLastRecord().getIndex();
	}

	public DBRecord getRecord(int i) throws MiniDbException {
		return db.getRecord(i);
	}

	public DBRecord getLastRecord() throws MiniDbException {
		return db.getLastRecord();
	}

	public long getDbFileSize() {
		return db.getTotalSize();
	}

	public int getDbRecordCount() {
		return db.getRecordCount();
	}

}
