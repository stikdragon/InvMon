package uk.co.stikman.invmon.datalog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.h2.util.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import uk.co.stikman.invmon.InvMonException;

public class TableImpl implements Table {

	public interface ConnectionSupplier {
		Connection get() throws SQLException;
	}

	private interface ApplyWhereFunc {
		void go(StringBuilder out, String name, Object val);
	}

	private static class KeyFieldThing {
		String			name;
		Object			applyUpdate;
		ApplyWhereFunc	applyWhere;
	}

	private final ConnectionSupplier	connSupp;
	private final JSONObject			desc;
	private final String				name;
	private List<KeyFieldThing>			keyFields	= new ArrayList<>();
	private List<String>				valueFields	= new ArrayList<>();
	private List<Object>				cachedRecord;
	private Object[]					cachedKey;

	public TableImpl(ConnectionSupplier newconn, JSONObject desc) {
		this.connSupp = newconn;
		this.desc = desc;
		this.name = desc.getString("id");

		JSONArray arr = desc.getJSONArray("fields");
		for (int i = 0; i < arr.length(); ++i) {
			JSONObject jo = arr.getJSONObject(i);
			if (jo.getBoolean("key")) {
				keyFields.add(new KeyFieldThing());
			} else {
				valueFields.add(jo.getString("id"));
			}
		}
	}

	@Override
	public float getFloat(Object[] key, String field) throws InvMonException {
		try (Connection conn = connSupp.get()) {
			PreparedStatement st = conn.prepareStatement("SELECT " + field + " FROM " + name + " WHERE " + generateWhereClause(key));
			ResultSet rs = st.executeQuery();
			if (!rs.next())
				return 0.0f;
			return rs.getFloat(1);
		} catch (SQLException e) {
			throw new InvMonException("Could not query Stats DB: " + e.getMessage(), e);
		}
	}

	private String generateWhereClause(Object[] keyvals) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (KeyFieldThing kft : keyFields)
			kft.applyWhere.go(sb, kft.name, keyvals[i++]);
		return sb.toString();
	}

	@Override
	public void setFloat(Object[] keyvals, String field, float val) throws InvMonException {
		/*
		 * MERGE INTO your_table_name (column1, column2, ...) KEY (key_column1,
		 * key_column2, ...) VALUES (value1, value2, ...);
		 */

		try (Connection conn = connSupp.get()) {
			StringBuilder sb = new StringBuilder();
			sb.append("MERGE INTO ").append(name).append(" (");
			String sep = "";
			for (KeyFieldThing kft : keyFields) {
				sb.append(sep).append(kft.name);
				sep = ", ";
			}
			sb.append(sep).append(field);

			sb.append(") KEY (");
			sep = "";
			for (KeyFieldThing kft : keyFields) {
				sb.append(sep).append(kft.name);
				sep = ", ";
			}
			sb.append(") VALUES (");
			int i = 0;
			sep = "";
			for (KeyFieldThing kft : keyFields) {
				Object k = keyvals[i++];
				if (k instanceof String)
					sb.append(sep).append("\"").append(StringUtils.quoteStringSQL((String) k)).append("\"");
				sep = ", ";
			}
			sb.append(sep).append(val);
			sb.append(")");

			PreparedStatement st = conn.prepareStatement(sb.toString());
			st.executeUpdate();
		} catch (SQLException e) {
			throw new InvMonException("Could not update Stats DB: " + e.getMessage(), e);
		}

	}

}
