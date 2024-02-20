package uk.co.stikman.invmon.datalog.stats;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datalog.StatsDB;
import uk.co.stikman.invmon.datamodel.FieldType;
import uk.co.stikman.invmon.datamodel.ModelField;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.table.DataRecord;
import uk.co.stikman.table.DataTable;
import uk.co.stikman.table.DataType;

public class SeasonalMaxStats extends StatsThing {

	public enum GroupingMode {
		MONTH,
		SEASON, // winter/spring etc
		YEAR
	}

	private GroupingMode			mode			= GroupingMode.MONTH;
	private int						samplesPerDay	= 24;
	private List<ModelField>		inputFields		= new ArrayList<>();
	private String					outputName;
	private final StatsDB			owner;
	private DataTable				building;
	private DataTable				data;
	private Map<String, DataRecord>	buildingCache;
	private Map<String, DataRecord>	dataIndex;
	private List<StatsField>		outputFields;

	public SeasonalMaxStats(String id, StatsDB owner, Element root) throws InvMonException {
		super(id);
		this.owner = owner;
		if (root.hasAttribute("grouping"))
			mode = GroupingMode.valueOf(root.getAttribute("grouping").toUpperCase());
		samplesPerDay = Integer.parseInt(InvUtil.getAttrib(root, "samplesPerDay", "24"));
		for (String s : InvUtil.getAttrib(root, "fields").split(",")) {
			s = s.trim();
			inputFields.add(owner.getOwner().getEnv().getModel().get(s));
		}

		outputName = InvUtil.getAttrib(root, "output");

		data = owner.fetchStoredTable("sms_" + id, true);
		if (data == null)
			data = createTable();

		outputFields = new ArrayList<>();
		int i = 2; // start at 2, 0 and 1 are the keys 
		for (ModelField f : inputFields) {
			StatsField sf = new StatsField(this, i, f.getId(), f.getType());
			outputFields.add(sf);
			++i;
		}
		outputFields.add(new StatsField(this, i, outputName, FieldType.POWER));
	}

	public String getOutputName() {
		return outputName;
	}

	private DataTable createTable() {
		DataTable dt = new DataTable();
		dt.addField("period", DataType.STRING);
		dt.addField("interval", DataType.INT);
		for (ModelField s : inputFields)
			dt.addField(s.getId(), DataType.DOUBLE);
		dt.addField(outputName, DataType.DOUBLE);
		return dt;
	}

	public GroupingMode getMode() {
		return mode;
	}

	public int getSamplesPerDay() {
		return samplesPerDay;
	}

	@Override
	public void beginBuild() throws InvMonException {
		if (building != null)
			throw new IllegalStateException("beginBuild has already been called");
		building = createTable();
		buildingCache = new HashMap<>();
	}

	@Override
	public void processRec(DBRecord rec) throws InvMonException {
		if (building == null)
			throw new IllegalStateException("beginBuild has not been called");

		Date date = new Date(rec.getTimestamp());
		SimpleDateFormat sdf = new SimpleDateFormat("MM");
		String periodKey = sdf.format(date);

		LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(rec.getTimestamp()), ZoneId.systemDefault());
		LocalDateTime midnight = LocalDateTime.of(dt.toLocalDate(), LocalTime.MIDNIGHT);
		long n = ChronoUnit.SECONDS.between(midnight, dt);
		int slot = (int) (n / (24 * 60 * 60 / samplesPerDay));

		String k = Integer.toString(slot) + "_" + periodKey;
		DataRecord r = buildingCache.get(k);
		if (r == null) {
			r = building.addRecord();
			r.setValue(0, periodKey);
			r.setValue(1, slot);
			buildingCache.put(k, r);
		}

		int i = 2;
		double sum = 0.0f;
		for (ModelField fld : inputFields) {
			double f = Math.max(r.getDouble(fld.getId()), rec.getFloat(fld));
			r.setValue(i++, f);
			sum += f;
		}
		r.setValue(i, sum); // set the final field
	}

	@Override
	public void finishBuild() throws InvMonException {
		if (building == null)
			throw new IllegalStateException("beginBuild has not been called");
		owner.storeTable("sms_" + getId(), building);
		synchronized (this) {
			data = building;
			dataIndex = null;
		}
	}

	@Override
	public StatsField getField(String id) {
		return outputFields.stream().filter(x -> x.getId().equals(id)).findAny().get();
	}

	@Override
	public float queryField(StatsField fld, long timestamp) {
		Map<String, DataRecord> lkp = getDataIndex();

		SimpleDateFormat sdf = new SimpleDateFormat("MM"); // i hate that these aren't thread safe
		String periodKey = sdf.format(timestamp);

		LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
		LocalDateTime midnight = LocalDateTime.of(ldt.toLocalDate(), LocalTime.MIDNIGHT);
		long n = ChronoUnit.SECONDS.between(midnight, ldt);
		double fslot = (double) n / (24 * 60 * 60 / samplesPerDay);
		int slot = (int) Math.floor(fslot);
		double mu = fslot - slot;

		DataRecord rec0 = lkp.get(periodKey + "_" + (slot));
		DataRecord rec1 = lkp.get(periodKey + "_" + (slot + 1));
		if (rec0 == null || rec1 == null)
			return 0.0f; // hmm what do

		float f0 = (float) rec0.getDouble(fld.getIndex());
		float f1 = (float) rec1.getDouble(fld.getIndex());

		return (float) ((f1 * mu) + f0 * (1.0 - mu));
	}

	private Map<String, DataRecord> getDataIndex() {
		synchronized (this) {
			if (dataIndex == null) {
				dataIndex = new HashMap<>();
				for (DataRecord r : data)
					dataIndex.put(r.getString(0) + "_" + r.getInt(1), r);
			}
			return dataIndex;
		}
	}

	@Override
	public List<StatsField> getOutputFields() {
		return outputFields;
	}

}
