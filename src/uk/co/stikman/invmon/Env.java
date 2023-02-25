package uk.co.stikman.invmon;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.stikman.eventbus.StringEventBus;
import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.datamodel.FieldType;
import uk.co.stikman.invmon.datamodel.RepeatSettings;
import uk.co.stikman.invmon.inverter.InvUtil;
import uk.co.stikman.log.ConsoleLogTarget;
import uk.co.stikman.log.Level;
import uk.co.stikman.log.StikLog;
import uk.co.stikman.table.DataTable;

public class Env {
	private static final StikLog	LOGGER		= StikLog.getLogger(Env.class);

	public FieldType DATATYPE_VOLT8 = null;

	private static String			version		= "dev";

	private List<InvModule>			parts		= new ArrayList<>();
	private long					nextId		= 0;
	private StringEventBus			bus			= new StringEventBus();
	private Thread					mainthread;
	private boolean					terminated	= false;
	private Config					config;
	private DataModel				model;
	private ExecutorService			exec;
	private Timer					timer;

	static {
		try (InputStream is = Env.class.getResourceAsStream("version.txt")) { // ant script writes this
			if (is != null)
				version = new String(InvUtil.readAll(is), StandardCharsets.UTF_8);
		} catch (IOException e) {
		}
	}

	public void start() throws InvMonException {
		try {
			StikLog.clearTargets();
			ConsoleLogTarget tgt = new ConsoleLogTarget();
			tgt.setFormat(new InvMonLogFormatter());
			tgt.enableLevel(Level.DEBUG, false);
			StikLog.addTarget(tgt);

			LOGGER.info("Starting InvMon...");
			listPorts();
			timer = new Timer();
			exec = Executors.newFixedThreadPool(4);
			bus.setImmediateMode(true);

			config = new Config();
			try {
				config.loadFromFile(Paths.get("config.xml").toFile());
			} catch (IOException e) {
				throw new InvMonException("Failed to load config: " + e.getMessage(), e);
			}

			model = new DataModel();
			try (InputStream is = getClass().getResourceAsStream("model.xml")) {
				RepeatSettings rs = new RepeatSettings();
				rs.setCountForGroup("inverters", config.getInverterCount());
				rs.setCountForGroup("batteries", config.getBatteryCount());
				model.setRepeatSettings(rs);
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

			LOGGER.info("Config loaded");
			for (InvModule part : parts) {
				LOGGER.info("Starting [" + part.getClass().getSimpleName() + "] module with id [" + part.getId() + "]");
				part.start();
			}

			LOGGER.info("Starting main thread");
			mainthread = new Thread(this::loop);
			mainthread.start();
			LOGGER.info("Ready");
		} catch (Throwable th) {
			try {
				terminate();
			} catch (Exception e) {
				System.err.println("Error during shutdown:");
				e.printStackTrace();
			}
			throw th;
		}
	}

	public static void listPorts() {
		//
		// print a list of serial ports in a nice table
		//
		LOGGER.info("Available serial ports:");
		DataTable dt = new DataTable();
		dt.addFields("Num", "Port", "Path", "Description");
		int i = 0;
		SerialPort[] ports = SerialPort.getCommPorts();
		for (SerialPort x : ports)
			dt.addRecord(Integer.toString(++i), x.getSystemPortName(), x.getSystemPortPath(), x.getDescriptivePortName());
		for (String s : dt.toString().split("\\n"))
			LOGGER.info(s);
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
		if (mainthread != null)
			mainthread.interrupt();

		if (timer != null)
			timer.cancel();

		if (exec != null) {
			exec.shutdown();
			try {
				if (!exec.awaitTermination(10, TimeUnit.SECONDS))
					LOGGER.error("Failed to shut down executor service");
			} catch (InterruptedException e1) {
				LOGGER.error("Failed to shut down executor service");
			}
		}

		for (InvModule part : parts)
			part.terminate();

		if (mainthread != null) {
			try {
				mainthread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		mainthread = null;
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

	@SuppressWarnings("unchecked")
	public <T extends InvModule> T findModule(String id) {
		for (InvModule m : parts) {
			if (id.equals(m.getId()))
				return (T) m;
		}
		return null;
	}

	public Config getConfig() {
		return config;
	}

	public static String getVersion() {
		return version;
	}

	public void submitTask(Runnable task) {
		exec.execute(task);
	}

	public void submitTimerTask(Runnable task, long period) {
		TimerTask tt = new TimerTask() {
			@Override
			public void run() {
				task.run();
			}
		};
		timer.scheduleAtFixedRate(tt, 0, period);
	}

}
