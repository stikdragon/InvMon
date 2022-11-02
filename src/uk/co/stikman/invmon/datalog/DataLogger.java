package uk.co.stikman.invmon.datalog;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.DataPoint;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.ProcessPart;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.inverter.InvUtil;

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
		db = new MiniDB(file);
		db.setModel(getEnv().getModel());
		try {
			db.open();
		} catch (IOException e) {
			throw new InvMonException("Failed to start database: " + e.getMessage(), e);
		}
	}

	@Subscribe(Events.POST_DATA)
	public void postData(PollData data) {
		//
		// insert record into database
		//
		Field fts = db.getModel().get("TIMESTAMP");
		DBRecord rec = db.addRecord();
		rec.set(fts, data.getTimestamp());
		for (DataPoint x : data.getData().values()) {
			for (Entry<Field, Object> e : x.getValues().entrySet()) {
				switch (e.getKey().getType()) {
					case FLOAT:
					case FREQ:
					case CURRENT:
					case POWER:
					case VOLTAGE:
						rec.set(e.getKey(), ((Number) e.getValue()).floatValue());
						break;
					case STRING:
						rec.set(e.getKey(), e.getValue().toString());
						break;
					case INT:
						rec.set(e.getKey(), ((Number) e.getValue()).intValue());
						break;
					default:
						throw new IllegalStateException("unsupported field type: " + e.getKey().getType());
				}
			}
		}
		db.commitRecord(rec);
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
