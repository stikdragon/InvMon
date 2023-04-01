package uk.co.stikman.invmon.htmlout;

public interface DataSetRecord {

	long getLong(int idx);

	float getFloat(int idx);
	
	int getInt(int idx);
	
	String getString(int idx);

}
