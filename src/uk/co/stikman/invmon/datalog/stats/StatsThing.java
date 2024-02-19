package uk.co.stikman.invmon.datalog.stats;

import java.util.List;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datalog.QueryRecord;
import uk.co.stikman.invmon.datamodel.ModelField;

public abstract class StatsThing {
	private final String id;

	public StatsThing(String id) {
		super();
		if (!id.matches("^[a-zA-Z0-9_]+$"))
			throw new IllegalArgumentException("Statistics IDs must be letters, digits, and _: " + id);
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public abstract void beginBuild() throws InvMonException;

	public abstract void processRec(DBRecord rec) throws InvMonException;

	public abstract void finishBuild() throws InvMonException;

	public abstract StatsField getField(String id);

	public abstract float queryField(StatsField fld, long timestamp);

	/**
	 * get list of the output field names (often the same as the normal list of
	 * fields)
	 * 
	 * @return
	 */
	public abstract List<String> getOutputFields();

}
