package uk.co.stikman.invmon.minidb;

public class ModelField {
	private final String	name;
	private DataType		type;

	public ModelField(String name, DataType type) {
		super();
		this.name = name;
		this.type = type;
	}

	public DataType getType() {
		return type;
	}

	public void setType(DataType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

}
