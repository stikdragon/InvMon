package uk.co.stikman.invmon.datamodel;

import java.util.Objects;
import java.util.function.Consumer;

import uk.co.stikman.invmon.datalog.DBRecord;

public class Field {
	private final String	id;
	private Field			parent;
	private FieldType		type;
	private AggregationMode	aggregationMode	= AggregationMode.SUM;
	private int				width;									// for strings
	private int				position;
	private int				offset;
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

	public int getOffset() {
		return offset;
	}

	public Field getParent() {
		return parent;
	}

	public int getPosition() {
		return position;
	}

	public FieldType getType() {
		return type;
	}

	public int getWidth() {
		return width;
	}

	public void setAggregationMode(AggregationMode aggregationMode) {
		this.aggregationMode = aggregationMode;
	}

	void setOffset(int offset) {
		this.offset = offset;
	}

	public void setParent(Field parent) {
		this.parent = parent;
	}

	void setPosition(int position) {
		this.position = position;
	}

	public void setType(FieldType type) {
		this.type = type;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	@Override
	public int hashCode() {
		return Objects.hash(aggregationMode, calculated, id, offset, parent, position, type, width);
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
		return aggregationMode == other.aggregationMode && Objects.equals(calculated, other.calculated) && Objects.equals(id, other.id) && offset == other.offset && Objects.equals(parent, other.parent) && position == other.position && type == other.type && width == other.width;
	}

	public Object toString(DBRecord r) {
		switch (this.getType()) {
			case FLOAT:
			case FREQ:
			case CURRENT:
			case POWER:
			case VOLTAGE:
				return Float.valueOf(r.getFloat(this));
			case STRING:
				return r.getString(this);
			case INT:
				return r.getInt(this);
			case TIMESTAMP:
				return Long.valueOf(r.getLong(this));
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
}
