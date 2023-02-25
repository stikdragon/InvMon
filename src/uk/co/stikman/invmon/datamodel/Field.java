package uk.co.stikman.invmon.datamodel;

import java.util.Objects;

import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datalog.QueryRecord;
import uk.co.stikman.invmon.datamodel.expr.CalcMethod;

public class Field {
	private final String	id;
	private FieldType		type;
	private AggregationMode	aggregationMode	= AggregationMode.SUM;
	private int				position;
	private String			calculated;
	private CalcMethod		calculationMethod;

	public Field(String id) {
		super();
		this.id = id;
	}

	public AggregationMode getAggregationMode() {
		return aggregationMode;
	}

	public String getId() {
		return id;
	}

	public int getPosition() {
		return position;
	}

	public FieldType getType() {
		return type;
	}

	public void setType(FieldType type) {
		this.type = type;
	}

	public void setAggregationMode(AggregationMode aggregationMode) {
		this.aggregationMode = aggregationMode;
	}

	/**
	 * position in respective type array in each record
	 * 
	 * @param position
	 */
	void setPosition(int position) {
		this.position = position;
	}

	@Override
	public int hashCode() {
		return Objects.hash(aggregationMode, calculated, id, position, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Field other = (Field) obj;
		return aggregationMode == other.aggregationMode && Objects.equals(calculated, other.calculated) && Objects.equals(id, other.id) && position == other.position && type == other.type;
	}

	public Object toString(DBRecord r) {
		if (this.getType() == FieldType.TIMESTAMP)
			return r.getTimestamp();
		switch (this.getType().getBaseType()) {
			case FLOAT:
				return Float.valueOf(r.getFloat(this));
			case STRING:
				return r.getString(this);
			case INT:
				return r.getInt(this);
			default:
				throw new RuntimeException("Unknown type: " + this.getType());
		}
	}

	public void setCalculated(String expression) {
		this.calculated = expression;
	}

	public String getCalculated() {
		return calculated;
	}

	public void setCalculationMethod(CalcMethod mthd) {
		this.calculationMethod = mthd;
	}

	public CalcMethod getCalculationMethod() {
		return calculationMethod;
	}

	@Override
	public String toString() {
		return id;
	}

	public float getFloat(QueryRecord rec) {
		if (this.getType().getBaseType() == FieldDataType.FLOAT)
			return rec.getFloat(position);
		throw new IllegalArgumentException("Not a float");
	}

}
