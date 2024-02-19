package uk.co.stikman.invmon.datalog.stats;

import uk.co.stikman.invmon.datamodel.FieldType;

public class StatsField {

	private String		id;
	private FieldType	type;
	private StatsThing	owner;
	private final int	index;

	public StatsField(StatsThing owner, int index, String id, FieldType type) {
		super();
		this.index = index;
		this.owner = owner;
		this.id = id;
		this.type = type;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return id;
	}

	public FieldType getType() {
		return type;
	}

	public StatsThing getStatsThing() {
		return owner;
	}

	public int getIndex() {
		return index;
	}

}
