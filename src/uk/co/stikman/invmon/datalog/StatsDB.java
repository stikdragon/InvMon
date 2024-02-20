package uk.co.stikman.invmon.datalog;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.datalog.stats.RebuildFrequency;
import uk.co.stikman.invmon.datalog.stats.SeasonalMaxStats;
import uk.co.stikman.invmon.datalog.stats.StatsThing;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.log.StikLog;
import uk.co.stikman.table.DataField;
import uk.co.stikman.table.DataRecord;
import uk.co.stikman.table.DataTable;
import uk.co.stikman.table.DataType;

public class StatsDB {
	private static final StikLog	LOGGER		= StikLog.getLogger(StatsDB.class);

	private final File				file;
	private final DataLogger		owner;
	private RebuildFrequency		rebuildFreq	= RebuildFrequency.DAILY;

	// we're setting this to now, so it doesn't always start to rebuild everything when you start up
	private long					lastRebuild	= System.currentTimeMillis();

	private List<StatsThing>		things		= new ArrayList<>();

	public StatsDB(DataLogger owner, File file) throws InvMonException {
		this.owner = owner;
		this.file = file;

		try (Connection c = getConnection()) {
			String configTable = null;
			ResultSet rs = c.getMetaData().getTables(null, null, null, new String[] { "TABLE" });
			while (rs.next()) {
				if ("__config".equalsIgnoreCase(rs.getString("TABLE_NAME")))
					configTable = "__config";
			}

			if (configTable == null)
				initialiseDb(c);

		} catch (SQLException e) {
			throw new InvMonException("Failed to opne StatsDB: " + e.getMessage(), e);
		}

	}

	public List<StatsThing> getThings() {
		return things;
	}

	public DataLogger getOwner() {
		return owner;
	}

	private void initialiseDb(Connection c) throws SQLException {
		String sql = """
				CREATE TABLE __config (
					k VARCHAR(32) PRIMARY KEY,
					v VARCHAR(255)
				)
				""";
		execSQL(c, sql);
		execSQL(c, "CREATE TABLE stats_tables (id VARCHAR(32) PRIMARY KEY, config VARCHAR(4000))");
		putSetting(c, "version", "1");
	}

	private static void putSetting(Connection c, String key, String val) throws SQLException {
		try (PreparedStatement st = c.prepareStatement("INSERT INTO __config (k, v) VALUES (?, ?)")) {
			st.setString(1, key);
			st.setString(2, val);
			st.executeUpdate();
		}
	}

	private static void execSQL(Connection c, String sql) throws SQLException {
		try (Statement st = c.createStatement()) {
			st.executeUpdate(sql);
		}
	}

	private synchronized Connection getConnection() throws SQLException {
		Connection conn;
		conn = DriverManager.getConnection("jdbc:h2:" + file.getAbsolutePath(), "", "");
		return conn;

	}

	public void configure(Element root) throws InvMonException {
		setRebuildFreq(RebuildFrequency.valueOf(InvUtil.getAttrib(root, "rebuild", "DAILY").toUpperCase()));
		
		for (Element el : InvUtil.getElements(root, "Stat")) {
			switch (InvUtil.getAttrib(el, "type")) {
				case "seasonal-max":
					StatsThing x = new SeasonalMaxStats(InvUtil.getAttrib(el, "id"), this, el);
					things.add(x);
					break;
				default:
					throw new InvMonException("Unknown Group type");
			}
		}
	}

	public void storeTable(String id, DataTable dt) throws InvMonException {
		JSONObject desc = new JSONObject();
		desc.put("id", id);
		JSONArray arr = new JSONArray();
		desc.put("fields", arr);
		for (int i = 0; i < dt.getFieldCount(); ++i) {
			DataField f = dt.getField(i);
			JSONObject jo = new JSONObject();
			jo.put("id", f.getName());
			jo.put("type", f.getType().name());
			arr.put(jo);
		}

		try (Connection c = getConnection()) {
			try (PreparedStatement st = c.prepareStatement("DELETE FROM stats_tables WHERE id = ?")) {
				st.setString(1, id);
				st.executeUpdate();
			}

			try (PreparedStatement st = c.prepareStatement("INSERT INTO stats_tables (id, config) VALUES (?, ?)")) {
				st.setString(1, desc.getString("id"));
				st.setString(2, desc.toString());
				st.executeUpdate();
			}

			//
			// now create the table and write data.  delete the old one first
			//
			try (PreparedStatement s = c.prepareStatement("DROP TABLE IF EXISTS \"" + id + "\"")) {
				s.executeUpdate();
			}

			SQLBuilder sb = new SQLBuilder();
			sb.append("CREATE TABLE ").appendName(id).append(" (");
			String sep = "";
			for (int i = 0; i < dt.getFieldCount(); ++i) {
				String t = switch (dt.getField(i).getType()) {
					case DOUBLE -> "FLOAT";
					case INT -> "INTEGER";
					case STRING -> "VARCHAR(200)";
					default -> throw new IllegalArgumentException("Unexpected value: " + dt.getField(i).getType());
				};
				sb.append(sep).appendName(dt.getField(i).getName()).append(" ").append(t).append(" ");
				sep = ", ";
			}
			sb.append(")");

			try (PreparedStatement st = c.prepareStatement(sb.toString())) {
				st.executeUpdate();
			}

			//
			// stuff data into it
			//
			sb = new SQLBuilder();
			sb.append("INSERT INTO ").appendName(id).append(" (");
			sep = "";
			for (int i = 0; i < dt.getFieldCount(); ++i) {
				sb.append(sep).appendName(dt.getField(i).getName());
				sep = ", ";
			}
			sb.append(") VALUES (");
			sep = "";
			for (int i = 0; i < dt.getFieldCount(); ++i) {
				sb.append(sep).append("?");
				sep = ", ";
			}
			sb.append(")");

			try (PreparedStatement st = c.prepareStatement(sb.toString())) {
				for (DataRecord r : dt) {
					for (int i = 0; i < dt.getFieldCount(); ++i) {
						switch (dt.getField(i).getType()) {
							case DOUBLE:
								st.setDouble(i + 1, r.getDouble(i));
								break;
							case INT:
								st.setInt(i + 1, r.getInt(i));
								break;
							case STRING:
								st.setString(i + 1, r.getString(i));
								break;
						}
					}
					st.executeUpdate();
				}
			}

		} catch (Exception e) {
			throw new InvMonException("FAiled to store table because: " + e.getMessage(), e);
		}

	}

	public DataTable fetchStoredTable(String id, boolean allowmissing) throws InvMonException {
		try {
			//
			// we have a table that describes the available tables, so check that
			//
			try (Connection c = getConnection()) {
				DataTable dt;
				try (PreparedStatement st = c.prepareStatement("SELECT config FROM stats_tables WHERE id = ?")) {
					st.setString(1, id);
					ResultSet rs = st.executeQuery();
					if (!rs.next()) {
						if (allowmissing)
							return null;
						else
							throw new NoSuchElementException("Stats table [" + id + "] does not exist");
					} else {
						JSONObject stored = new JSONObject(rs.getString("config"));

						//
						// create a datatable to represent this
						//
						dt = new DataTable();
						JSONArray arr = stored.getJSONArray("fields");
						for (int i = 0; i < arr.length(); ++i) {
							JSONObject jo = arr.getJSONObject(i);
							DataType typ = DataType.valueOf(jo.getString("type").toUpperCase());
							dt.addField(jo.getString("id"), typ);
						}
					}
				}

				//
				// read data from that table
				//
				SQLBuilder sb = new SQLBuilder();
				sb.append("SELECT ");
				String sep = "";
				for (int i = 0; i < dt.getFieldCount(); ++i) {
					sb.append(sep).appendName(dt.getField(i).getName());
					sep = ", ";
				}
				sb.append(" FROM ").appendName(id);
				try (PreparedStatement st = c.prepareStatement(sb.toString())) {
					ResultSet rs = st.executeQuery();
					while (rs.next()) {
						DataRecord r = dt.addRecord();
						for (int i = 0; i < dt.getFieldCount(); ++i) {
							switch (dt.getField(i).getType()) {
								case DOUBLE:
									r.setValue(i, rs.getDouble(i + 1));
									break;
								case INT:
									r.setValue(i, rs.getInt(i + 1));
									break;
								case STRING:
									r.setValue(i, rs.getString(i + 1));
									break;
								default:
									break;
							}
						}
					}
				}
				return dt;
			}

		} catch (Exception e) {
			throw new InvMonException("Failed to get stored table: " + e.getMessage(), e);
		}
	}

	private static final int	PAUSE_INTERVAL	= 500;
	private static final int	PAUSE_LENGTH	= 100;

	public void rebuild(boolean gently, Consumer<String> message) throws Exception {
		long nextPause = System.currentTimeMillis() + PAUSE_INTERVAL;
		for (StatsThing thing : things) {
			thing.beginBuild();
			try {

				//
				// loop through every record in MiniDB 
				//
				double rate = 1.0;
				long startedAt = System.currentTimeMillis();
				int cnt = owner.getDbRecordCount();
				for (int i = 0; i < cnt; ++i) {
					if (System.currentTimeMillis() >= nextPause) {
						if (gently)
							Thread.sleep(PAUSE_LENGTH);
						nextPause = System.currentTimeMillis() + PAUSE_INTERVAL;
						//
						// also update message here
						//
						double pct = (double) i / cnt;
						double dt = (System.currentTimeMillis() - startedAt) / 1000.0;
						if (dt == 0.0)
							dt = 0.0001;
						double remain = ((1.0 / pct) - 1) * dt;
						message.accept(String.format("Processing [%s] record %d / %d - %2d%%.  Approx remaining: %d seconds", thing.getId(), i, cnt, (int) (i * 100 / cnt), (int)remain));
					}
					DBRecord rec = owner.getRecord(i);
					thing.processRec(rec);
				}
			} finally {
				thing.finishBuild();
			}
		}
	}

	public StatsThing getThing(String name) {
		return things.stream().filter(x -> name.equals(x.getId())).findFirst().get();
	}

	public RebuildFrequency getRebuildFreq() {
		return rebuildFreq;
	}

	public void setRebuildFreq(RebuildFrequency rebuildFreq) {
		this.rebuildFreq = rebuildFreq;
	}

	public long getLastRebuild() {
		return lastRebuild;
	}

	public void setLastRebuild(long lastRebuild) {
		this.lastRebuild = lastRebuild;
	}

	public boolean shouldRebuild() {
		return System.currentTimeMillis() - lastRebuild >= getRebuildTime();
	}

	private long getRebuildTime() {
		if (rebuildFreq == RebuildFrequency.DAILY)
			return 60 * 60 * 24 * 1000;
		throw new IllegalStateException("Unknown rebuild freq: " + rebuildFreq);
	}

}
