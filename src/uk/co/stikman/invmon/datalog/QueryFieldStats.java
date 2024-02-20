package uk.co.stikman.invmon.datalog;

import uk.co.stikman.invmon.datalog.stats.StatsField;
import uk.co.stikman.invmon.datamodel.FieldType;

public class QueryFieldStats extends QueryField {

	private StatsField	field;
	private int			index;

	public QueryFieldStats(StatsField f) {
		this.field = f;
	}

	@Override
	protected String getId() {
		return field.getOwner().getId() + "." + field.getId();
	}

	@Override
	protected FieldType getType() {
		return field.getType();
	}

	public StatsField getField() {
		return field;
	}

	public void setIndex(int i) {
		this.index = i;
	}

	public int getIndex() {
		return index;
	}

}
