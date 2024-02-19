package uk.co.stikman.invmon.datalog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import uk.co.stikman.invmon.InvMonException;

public class TableImpl implements Table {

	public interface ConnectionSupplier {
		Connection get() throws SQLException;
	}

	private enum Typ {
		STRING, INT, FLOAT
	}

	private static class KeyFieldThing {
		final String	name;
		final Typ		type;

		public KeyFieldThing(String name, Typ type) {
			super();
			this.name = name;
			this.type = type;
		}

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
			if (jo.optBoolean("key")) {
				Typ t = Typ.valueOf(jo.getString("type").toUpperCase());
				keyFields.add(new KeyFieldThing(jo.getString("id"), t));
			} else {
				valueFields.add(jo.getString("id"));
			}
		}
	}

	@Override
	public float getFloat(Object[] key, String field) throws InvMonException {
		try (Connection conn = connSupp.get()) {
			PreparedStatement st = conn.prepareStatement("SELECT \"f_" + field + "\" FROM " + name + " WHERE " + generateWhereClause(key));
			ResultSet rs = st.executeQuery();
			if (!rs.next())
				return 0.0f;
			return rs.getFloat(1);
		} catch (SQLException e) {
			throw new InvMonException("Could not query Stats DB: " + e.getMessage(), e);
		}
	}

	private String generateWhereClause(Object[] keyvals) {
		SQLBuilder sb = new SQLBuilder();
		int i = 0;
		String sep = "";
		for (KeyFieldThing kft : keyFields) {
			sb.append(sep);
			sb.append("(");
			sb.appendName(kft.name).append(" = ");
			switch (kft.type) {
				case STRING:
					sb.appendEscaped((String) keyvals[i++]).append(")");
					break;
				default:
					sb.append(keyvals[i++].toString()).append(")");
					break;

			}
			sep = " AND ";
		}
		return sb.toString();
	}

	@Override
	public void setFloat(Object[] keyvals, String field, float val) throws InvMonException {
		/*
		 * MERGE INTO your_table_name (column1, column2, ...) KEY (key_column1,
		 * key_column2, ...) VALUES (value1, value2, ...);
		 */
		try (Connection conn = connSupp.get()) {
			SQLBuilder sb = new SQLBuilder();
			sb.append("MERGE INTO ").appendName(name).append(" (");
			String sep = "";
			for (KeyFieldThing kft : keyFields) {
				sb.append(sep).appendName(kft.name);
				sep = ", ";
			}
			sb.append(sep).appendName("f_" + field);

			sb.append(") KEY (");
			sep = "";
			for (KeyFieldThing kft : keyFields) {
				sb.append(sep).appendName(kft.name);
				sep = ", ";
			}
			sb.append(") VALUES (");
			int i = 0;
			sep = "";
			for (KeyFieldThing kft : keyFields) {
				Object k = keyvals[i++];
				sb.append(sep);
				if (k instanceof String)
					sb.appendEscaped((String) k);
				else
					sb.append(k.toString());
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
