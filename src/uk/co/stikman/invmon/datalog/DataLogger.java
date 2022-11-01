package uk.co.stikman.invmon.datalog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.InverterDataPoint;
import uk.co.stikman.invmon.ProcessPart;
import uk.co.stikman.invmon.inverter.InvUtil;
import uk.co.stikman.invmon.minidb.MiniDB;

public class DataLogger extends ProcessPart {

	private String	filename;
	private MiniDB	db;
	private File	lock;
	private File	file;

	public DataLogger(String id, Env env) {
		super(id, env);
	}

	public void start() throws InvMonException {
		super.start();
		db = new MiniDB();
		try (InputStream is = getClass().getResourceAsStream("model.xml")) {
			db.loadModel(is);
		} catch (IOException e) {
			throw new InvMonException("Could not load DataLogger model: " + e.getMessage(), e);
		}

	}

	@Override
	public void configure(Element config) {
		file = new File(InvUtil.getAttrib(config, "file"));
		lock = new File(file.getAbsolutePath() + ".lock");
	}

	@Override
	public void terminate() {
		super.terminate();
	}

}
