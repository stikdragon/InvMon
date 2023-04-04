package uk.co.stikman.invmon.htmlout;

import java.time.ZoneId;
import java.util.List;

import org.json.JSONObject;

public interface DataSet {

	long getStart();
	long getEnd();
	int getFieldIndex(String name);
	List<DataSetRecord> getRecords();
	ZoneId getZone();

	JSONObject toJSON();

}
