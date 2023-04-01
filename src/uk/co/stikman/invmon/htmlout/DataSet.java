package uk.co.stikman.invmon.htmlout;

import java.util.List;

import org.json.JSONObject;

public interface DataSet {

	long getStart();
	long getEnd();
	int getFieldIndex(String name);
	List<DataSetRecord> getRecords();

	JSONObject toJSON();

}
