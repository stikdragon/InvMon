package uk.co.stikman.invmon.datalog.stats;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datalog.StatsDB;
import uk.co.stikman.invmon.datalog.Table;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.inverter.util.InvUtil;

public class SeasonalMaxStats extends StatsThing {

	public enum GroupingMode {
		MONTH,
		SEASON, // winter/spring etc
		YEAR
	}

	private GroupingMode		mode			= GroupingMode.MONTH;
	private int					samplesPerDay	= 24;
	private String				prefix			= "stats";
	private List<String>		fields			= new ArrayList<>();
	private final StatsDB		owner;

	//
	// cached things
	//
	private transient Table		table;
	private transient Field[]	sampleFields;

	public SeasonalMaxStats(String id, StatsDB owner, Element root) {
		super(id);
		this.owner = owner;
		prefix = InvUtil.getAttrib(root, "outputPrefix");
		if (root.hasAttribute("grouping"))
			mode = GroupingMode.valueOf(root.getAttribute("grouping").toUpperCase());
		samplesPerDay = Integer.parseInt(InvUtil.getAttrib(root, "samplesPerDay", "24"));
		for (String s : InvUtil.getAttrib(root, "fields").split(",")) {
			s = s.trim();
			fields.add(s);
		}
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
	public void update(DBRecord rec) throws InvMonException {
		if (sampleFields == null) {
			sampleFields = new Field[fields.size()];
			int i = 0;
			for (String s : fields)
				sampleFields[i++] = owner.getOwner().getEnv().getModel().get(s);
		}

		try {
			Table tbl = getTable();

			Date date = new Date(rec.getTimestamp());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
			String periodKey = sdf.format(date);

			LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(rec.getTimestamp()), ZoneId.systemDefault());
			LocalDateTime midnight = LocalDateTime.of(dt.toLocalDate(), LocalTime.MIDNIGHT);
			long n = ChronoUnit.SECONDS.between(midnight, dt);
			int slot = (int) (n / samplesPerDay);

			Object[] key = new Object[] { periodKey, Integer.valueOf(slot) };

			for (Field f : sampleFields) {
				float sampl = rec.getFloat(f);
				float accum = tbl.getFloat(key, f.getId());
				tbl.setFloat(key, f.getId(), Math.max(sampl, accum));
			}
		} catch (Exception e) {
			throw new InvMonException("Failed to update stats record: " + e.getMessage(), e);
		}
	}

	private Table getTable() throws InvMonException {
		if (table != null)
			return table;

		//
		// need to work out the key for this table
		// Table has structure:
		//   [period] string
		//   [slot]   int
		//   [fld1]   float
		//   [fld2]   float
		//    etc
		//
		//

		//
		// find (or create) the appropriate table
		//
		JSONObject jo = new JSONObject();
		JSONArray arr = new JSONArray();
		jo.put("id", "sms-" + getId());
		jo.put("fields", arr);
		arr.put(new JSONObject().put("id", "period").put("type", "string").put("key", true));
		arr.put(new JSONObject().put("id", "interval").put("type", "int").put("key", true));
		for (String fld : fields)
			arr.put(new JSONObject().put("id", "f_" + fld).put("type", "float"));
		table = owner.getTable(jo, true);
		return table;
	}

}
