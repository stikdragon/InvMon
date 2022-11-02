package uk.co.stikman.invmon.datamodel;

import java.util.Objects;

import uk.co.stikman.invmon.datalog.DBRecord;

public class Field {
	private final String	id;
	private Field			parent;
	private FieldType		type;
	private int				width;		// for strings
	private int				position;
	private int				offset;

	public Field(String id) {
		super();
		this.id = id;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getOffset() {
		return offset;
	}

	void setOffset(int offset) {
		this.offset = offset;
	}

	public int getPosition() {
		return position;
	}

	void setPosition(int position) {
		this.position = position;
	}

	public String getId() {
		return id;
	}

	public Field getParent() {
		return parent;
	}

	public void setParent(Field parent) {
		this.parent = parent;
	}

	public FieldType getType() {
		return type;
	}

	public void setType(FieldType type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, parent, position, type, width);
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
		return Objects.equals(id, other.id) && Objects.equals(parent, other.parent) && position == other.position && type == other.type && width == other.width;
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

}
