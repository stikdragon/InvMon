package uk.co.stikman.invmon.datalog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.InverterDataPoint;
import uk.co.stikman.invmon.ProcessPart;
import uk.co.stikman.invmon.RecordListener;
import uk.co.stikman.invmon.inverter.InverterUtils;
import uk.co.stikman.invmon.minidb.MiniDB;

public class DataLogger implements ProcessPart, RecordListener {

	private String	filename;
	private String	id;
	private Env		env;
	private MiniDB	db;
	private File	lock;
	private File	file;

	public DataLogger(Env env, String id) {
		this.env = env;
		this.id = id;
	}

	public void start() throws InvMonException {
		
		db = new MiniDB();
		try (InputStream is = getClass().getResourceAsStream("model.xml")) {
			db.loadModel(is);
		} catch (IOException e) {
			throw new InvMonException("Could not load DataLogger model: " + e.getMessage(), e);
		}
		
		env.addListener(this);

	}

	public void stop() {

	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void configure(Element config) {
		file = new File(InverterUtils.getAttrib(config, "file"));
		lock = new File(file.getAbsolutePath() + ".lock");
	}

	@Override
	public void terminate() {
	}

	@Override
	public void record(long id, InverterDataPoint rec) {

	}

}
