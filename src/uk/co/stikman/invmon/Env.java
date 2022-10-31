package uk.co.stikman.invmon;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import uk.co.stikman.invmon.datalog.DataLogger;

public class Env {
	private List<ProcessPart>		parts			= new ArrayList<>();
	private List<RecordListener>	recordListeners	= new ArrayList<>();
	private long					nextId			= 0;

	public void postRecord(InverterDataPoint rec) {
		synchronized (this) {
			long id = ++nextId;
			for (RecordListener l : recordListeners)
				l.record(id, rec);
		}
	}

	public void addListener(RecordListener l) {
		synchronized (this) {
			recordListeners.add(l);
		}
	}

	public void start() {
		Config conf = new Config();
		try {
			conf.loadFromFile(Paths.get("conf", "config.xml").toFile());
		} catch (IOException e) {
			throw new RuntimeException("Failed to load config: " + e.getMessage(), e);
		}

		for (ProcessPartDefinition def : conf.getThings()) {
			try {
				ProcessPart part = def.getClazz().getConstructor(String.class, Env.class).newInstance(def.getId(), this);
				part.configure(def.getConfig());
				parts.add(part);
			} catch (Exception e) {
				throw new RuntimeException("Failed to start part [" + def.getId() + "]: " + e.getMessage(), e);
			}
		}

		for (ProcessPart part : parts)
			part.start();

		DataLogger logger = new DataLogger();
		logger.setDatabaseFile("conf//inverterdata.db");
	}

	public void terminate() {
		// todo..
		for (ProcessPart part : parts)
			part.terminate();
	}

	public void awaitTermination() {
		// TODO...
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
