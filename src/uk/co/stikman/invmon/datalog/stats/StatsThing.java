package uk.co.stikman.invmon.datalog.stats;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.datalog.DBRecord;

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

	public abstract void update(DBRecord rec) throws InvMonException;

}
