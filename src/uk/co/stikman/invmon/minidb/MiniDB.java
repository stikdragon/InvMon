package uk.co.stikman.invmon.minidb;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MiniDB {
	private Model			model;
	private List<DBRecord>	records	= new ArrayList<>();
	
	public void loadModel(InputStream is) {
		model = new Model();
		model.loadXML(is);
	}
	
	
}
