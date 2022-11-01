package uk.co.stikman.invmon;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import uk.co.stikman.eventbus.StringEventBus;

public class Env {
	private List<ProcessPart>	parts		= new ArrayList<>();
	private long				nextId		= 0;
	private StringEventBus		bus			= new StringEventBus();
	private Thread				mainthread;
	private boolean				terminated	= false;
	private Config				config;

	public void start() throws InvMonException {
		bus.setImmediateMode(true);
		
		config = new Config();
		try {
			config.loadFromFile(Paths.get("conf", "config.xml").toFile());
		} catch (IOException e) {
			throw new RuntimeException("Failed to load config: " + e.getMessage(), e);
		}

		for (ProcessPartDefinition def : config.getThings()) {
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

		mainthread = new Thread(this::loop);
		mainthread.start();
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

		// todo..
		for (ProcessPart part : parts)
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

}
