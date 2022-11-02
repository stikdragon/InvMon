package uk.co.stikman.invmon.datamodel;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MiniDB {
	private DataModel			model;
	private List<DBRecord>	records	= new ArrayList<>();
	private long			modelHash;

	public void loadModel(InputStream is) throws IOException {
		model = new DataModel();
		model.loadXML(is);
		modelHash = model.hashCode();
	}

}
