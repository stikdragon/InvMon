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

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.datalog.stats.SeasonalMaxStats;
import uk.co.stikman.invmon.datalog.stats.StatsThing;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.log.StikLog;

public class StatsDB {
	private static final StikLog	LOGGER			= StikLog.getLogger(StatsDB.class);

	private final File				file;
	private final DataLogger		owner;

	private List<StatsOutputField>	outputFields	= new ArrayList<>();
	private List<StatsThing>		things			= new ArrayList<>();

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

	public void update(DBRecord rec) throws InvMonException {
		for (StatsThing x : things)
			x.update(rec);
	}

	public void configure(Element root) throws InvMonException {
		for (Element el : InvUtil.getElements(root, "Group")) {
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

	/**
	 * look for the table described by <code>desc</code>. If it exists but isn't
	 * compatible then it'll throw an exception. If it doesn't exist then it'll
	 * create it if you specify true, otherwise {@link NoSuchElementException}
	 * 
	 * @param config
	 * @return
	 * @throws InvMonException
	 */
	public Table getTable(JSONObject desc, boolean createIfMissing) throws InvMonException {
		//
		// we have a table that describes the available tables, so check that
		//
		try (Connection c = getConnection()) {
			try (PreparedStatement st = c.prepareStatement("SELECT config FROM stats_tables WHERE id = ?")) {
				st.setString(1, desc.getString("id"));
				ResultSet rs = st.executeQuery();
				if (!rs.next()) {
					if (createIfMissing)
						createTable(c, desc);
					throw new NoSuchElementException("Stats table [" + desc.getString("id") + "] does not exist");
				}
				JSONObject stored = new JSONObject(rs.getString("config"));

				//
				// check they are compatible
				//
				if (!desc.equals(stored)) {
					LOGGER.error("Stored: " + stored.toString());
					LOGGER.error("  Desc: " + desc.toString());
					throw new InvMonException("Stored table is not compatible for [" + desc.getString("id") + "]");
				}
			}

			return new TableImpl(() -> getConnection(), desc);

		} catch (SQLException e) {
			throw new InvMonException("Failed to get table: " + e.getMessage(), e);
		}

	}

	private void createTable(Connection c, JSONObject desc) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("CREATE TABLE %s (", desc.getString("id")));
		JSONArray arr = desc.getJSONArray("fields");
		String sep = "";
		for (int i = 0; i < arr.length(); ++i) {
			sb.append(sep);
			sep = ", ";
			JSONObject jo = arr.getJSONObject(i);
			sb.append(jo.getString("id")).append(" ");
			sb.append(switch (jo.getString("type")) {
				case "float" -> "FLOAT";
				case "int" -> "INTEGER";
				default -> throw new IllegalArgumentException("Unknown type: " + jo.getString("type"));
			});
		}
		sb.append(")");

		try (PreparedStatement st = c.prepareStatement(sb.toString())) {
			st.executeUpdate();
		}

		//
		// now insert this into the stats_tables table too
		//

		sb = new StringBuilder();

		try (PreparedStatement st = c.prepareStatement("INSERT INTO stats_tables (id, config) VALUES (?, ?)")) {
			st.setString(1, desc.getString("id"));
			st.setString(2, desc.toString());
			st.executeUpdate();
		}
	}

}
