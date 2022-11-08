package uk.co.stikman.invmon;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.stikman.eventbus.StringEventBus;
import uk.co.stikman.invmon.datamodel.DataModel;

public class Env {
	public static final String	VERSION		= "0.2";

	private List<InvModule>		parts		= new ArrayList<>();
	private long				nextId		= 0;
	private StringEventBus		bus			= new StringEventBus();
	private Thread				mainthread;
	private boolean				terminated	= false;
	private Config				config;
	private DataModel			model;

	public void start() throws InvMonException {
		for (SerialPort port : SerialPort.getCommPorts()) {
		}
		
		bus.setImmediateMode(true);

		config = new Config();
		try {
			config.loadFromFile(Paths.get("conf", "config.xml").toFile());
		} catch (IOException e) {
			throw new InvMonException("Failed to load config: " + e.getMessage(), e);
		}

		model = new DataModel();
		try (InputStream is = getClass().getResourceAsStream("model.xml")) {
			model.loadXML(is);
		} catch (IOException e) {
			throw new InvMonException("Failed to load model: " + e.getMessage(), e);
		}

		for (InvModDefinition def : config.getThings()) {
			try {
				InvModule part = def.getClazz().getConstructor(String.class, Env.class).newInstance(def.getId(), this);
				part.configure(def.getConfig());
				parts.add(part);
			} catch (Exception e) {
				throw new InvMonException("Failed to start part [" + def.getId() + "]: " + e.getMessage(), e);
			}
		}

		for (InvModule part : parts)
			part.start();

		mainthread = new Thread(this::loop);
		mainthread.start();
	}

	public DataModel getModel() {
		return model;
	}

	private void loop() {

		for (;;) {
			if (terminated)
				return;

			PollData data = new PollData();
			bus.fire(Events.POLL_SOURCES, data);
			bus.fire(Events.POST_DATA, data);

			try {
				Thread.sleep(config.getUpdatePeriod());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public void terminate() {
		terminated = true;
		mainthread.interrupt();

		for (InvModule part : parts)
			part.terminate();

		try {
			mainthread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		mainthread = null;
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

	public StringEventBus getBus() {
		return bus;
	}

	@SuppressWarnings("unchecked")
	public <T extends InvModule> T getModule(String id) {
		for (InvModule m : parts) {
			if (id.equals(m.getId()))
				return (T) m;
		}
		throw new NoSuchElementException("Module [" + id + "] not found");
	}

}
