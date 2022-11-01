package uk.co.stikman.invmon.minidb;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MiniDB {
	private Model			model;
	private List<DBRecord>	records	= new ArrayList<>();
	private long			modelHash;

	public void loadModel(InputStream is) throws IOException {
		model = new Model();
		model.loadXML(is);
		modelHash = model.hashCode();
	}

}
