package uk.co.stikman.invmon.datalog.stats;

import java.util.List;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.datalog.DBRecord;

public abstract class StatsThing {
	private final String		id;

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

	/**
	 * get the value of the given field at a particular timestamp
	 * 
	 * @param fld
	 * @param timestamp
	 * @return
	 */
	public abstract float queryField(StatsField fld, long timestamp);

	/**
	 * get list of the output fields that this one provides (this is probably
	 * generated at least partially from the list of input fields)
	 * 
	 * @return
	 */
	public abstract List<StatsField> getOutputFields();


}
