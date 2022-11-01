package uk.co.stikman.invmon.minidb;

public class Field {
	private final String	id;
	private Field			parent;
	private FieldType		type;
	private boolean			key;

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

	public Field(String id) {
		super();
		this.id = id;
	}

	public void setType(FieldType type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + (key ? 1231 : 1237);
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
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
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (key != other.key)
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	public boolean isKey() {
		return key;
	}

	public void setKey(boolean key) {
		this.key = key;
	}

}
