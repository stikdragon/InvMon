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
import java.util.NoSuchElementException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datalog.QueryRecord;
import uk.co.stikman.invmon.datalog.StatsDB;
import uk.co.stikman.invmon.datalog.Table;
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
	private String					prefix			= "stats";
	private List<ModelField>		fields			= new ArrayList<>();
	private final StatsDB			owner;
	private DataTable				building;
	private DataTable				data;
	private Map<String, DataRecord>	buildingCache;
	private Map<String, DataRecord>	dataIndex;

	public SeasonalMaxStats(String id, StatsDB owner, Element root) throws InvMonException {
		super(id);
		this.owner = owner;
		prefix = InvUtil.getAttrib(root, "outputPrefix");
		if (root.hasAttribute("grouping"))
			mode = GroupingMode.valueOf(root.getAttribute("grouping").toUpperCase());
		samplesPerDay = Integer.parseInt(InvUtil.getAttrib(root, "samplesPerDay", "24"));
		for (String s : InvUtil.getAttrib(root, "fields").split(",")) {
			s = s.trim();
			fields.add(owner.getOwner().getEnv().getModel().get(s));
		}

		data = owner.fetchStoredTable("sms_" + id, true);
		if (data == null)
			data = createTable();

	}

	private DataTable createTable() {
		DataTable dt = new DataTable();
		dt.addField("period", DataType.STRING);
		dt.addField("interval", DataType.INT);
		for (ModelField s : fields)
			dt.addField(s.getId(), DataType.DOUBLE);
		return dt;
	}

	public GroupingMode getMode() {
		return mode;
	}

	public int getSamplesPerDay() {
		return samplesPerDay;
	}

	public String getPrefix() {
		return prefix;
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
		for (ModelField fld : fields)
			r.setValue(i++, Math.max(r.getDouble(fld.getId()), rec.getFloat(fld)));
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
		int i = 0;
		for (ModelField f : fields) {
			if (f.getId().equals(id)) {
				StatsField sf = new StatsField(this, i, f.getId(), f.getType());
				return sf;
			}
			++i;
		}
		throw new NoSuchElementException("StatsField [" + id + "] not found");
	}

	@Override
	public float queryField(StatsField fld, long timestamp) {
		Map<String, DataRecord> lkp = getDataIndex();
		DataTable dat = data;

		SimpleDateFormat sdf = new SimpleDateFormat("MM"); // i hate that these aren't thread safe
		String periodKey = sdf.format(timestamp);

		LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
		LocalDateTime midnight = LocalDateTime.of(ldt.toLocalDate(), LocalTime.MIDNIGHT);
		long n = ChronoUnit.SECONDS.between(midnight, ldt);
		int slot = (int) (n / (24 * 60 * 60 / samplesPerDay));

		String k = periodKey + "_" + slot;
		DataRecord rec = lkp.get(k);
		if (rec == null)
			return 0.0f; // hmm what do
		return (float) rec.getDouble(fld.getIndex());
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
	public List<String> getOutputFields() {
		return fields.stream().map(x -> x.getId()).toList();
	}

}
