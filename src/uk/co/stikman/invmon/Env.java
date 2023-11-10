package uk.co.stikman.invmon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
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
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.log.ConsoleLogTarget;
import uk.co.stikman.log.Level;
import uk.co.stikman.log.StikLog;
import uk.co.stikman.table.DataTable;

public class Env {
	private static final StikLog			LOGGER					= StikLog.getLogger(Env.class);
	private static final StikLog			UL_LOGGER				= StikLog.getLogger("UserLog");
	private static final int				MAX_USER_LOG_LENGTH		= 500;
	private static final DateTimeFormatter	DTF						= DateTimeFormatter.ofPattern("MMM dd HH:mm");

	public FieldType						DATATYPE_VOLT8			= null;

	private static String					version					= "dev";

	private List<InvModule>					parts					= new ArrayList<>();
	private StringEventBus					bus						= new StringEventBus();
	private Thread							mainthread;
	private boolean							terminated				= false;
	private Config							config;
	private DataModel						model;
	private ExecutorService					exec;
	private Timer							timer;
	private LinkedList<String>				userLog					= new LinkedList<>();

	private EnvLog							log;

	private File							root;
	private RateLimiter						mainLoopErrorRateLimit	= new RateLimiter(300, 600);					// 300 errors in 10 minutes will shut it down

	static {
		try (InputStream is = Env.class.getResourceAsStream("version.txt")) { // ant script writes this
			if (is != null)
				version = new String(InvUtil.readAll(is), StandardCharsets.UTF_8);
		} catch (IOException e) {
		}
	}

	public void start(File root) throws InvMonException {
		try {
			this.root = root;
			config = new Config();
			try {
				config.loadFromFile(new File(root, "config.xml"));
			} catch (IOException e) {
				throw new InvMonException("Failed to load config: " + e.getMessage(), e);
			}

			StikLog.clearTargets();
			ConsoleLogTarget tgt = new ConsoleLogTarget();
			tgt.setFormat(new InvMonLogFormatter());
			tgt.enableLevel(Level.DEBUG, config.isLogDebug());
			StikLog.addTarget(tgt);

			log = new EnvLog();
			log.setFormat(new InvMonLogFormatter());
			log.enableLevel(Level.DEBUG, false);
			StikLog.addTarget(log);

			if (config.getLogFileName() != null) {
				try {
					FileLogTarget flt = new FileLogTarget(new File(config.getLogFileName()));
					flt.setFormat(new InvMonLogFormatter());
					flt.enableLevel(Level.DEBUG, config.isLogDebug());
					StikLog.addTarget(flt);
				} catch (IOException e) {
					throw new InvMonException("Failed to start up file logger: " + e.getMessage(), e);
				}
			}

			LOGGER.info("Starting InvMon...");
			listPorts();
			timer = new Timer();
			exec = Executors.newFixedThreadPool(4);
			bus.setImmediateMode(true);

			//
			// load model and configure it for the combination of devices we've got
			// enabled in the config file
			//
			model = new DataModel();
			File f = config.getModelFile();
			if (!f.isAbsolute())
				f = new File(root, f.toString());
			LOGGER.info("Loading model from: " + f);
			try (InputStream is = new FileInputStream(f)) {
				LOGGER.info("Model generation settings:");
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
					throw new InvMonException("Failed to start module [" + def.getId() + "]: " + e.getMessage(), e);
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
		long lastTimer = System.currentTimeMillis();
		long t2err = 0;

		userLog(null, "InvMon started");

		for (;;) {
			if (terminated)
				return;

			try {

				//
				// do timer events
				//
				bus.fire(Events.TIMER_UPDATE_PERIOD);

				long dt = System.currentTimeMillis() - lastTimer;
				lastTimer = System.currentTimeMillis();
				t2err += dt;
				if (t2err >= 60 * 1000) {
					while (t2err >= 60 * 1000)
						t2err -= 60 * 1000;
					bus.fire(Events.TIMER_UPDATE_MINUTE);
				}

				//
				// poll inverters and post data to anything listening
				//
				PollData data = new PollData();
				bus.fire(Events.POLL_SOURCES, data);
				bus.fire(Events.POST_DATA, data);
			} catch (Exception e) {
				LOGGER.error(e);
				mainLoopErrorRateLimit.trigger();
				if (mainLoopErrorRateLimit.test()) {
					LOGGER.error("!!! -------------------------------");
					LOGGER.error("ERROR RATE LIMIT EXCEEDED, QUITTING");
					LOGGER.error("!!! -------------------------------");
					break;
				}

			}

			//
			// wait for the update period 
			//
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

	public synchronized Config getConfig() {
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

	public Iterable<InvModule> getModules() {
		return parts;
	}

	public EnvLog getLog() {
		return log;
	}

	public File getRoot() {
		return root;
	}

	public File getFile(String path) {
		File f = new File(path);
		if (f.isAbsolute())
			return f;
		return new File(root, path);
	}

	public void userLog(InvModule sender, String msg) {
		UL_LOGGER.info((sender == null ? "" : (sender.getId() + "/")) + msg);
		synchronized (userLog) {
			while (userLog.size() > MAX_USER_LOG_LENGTH)
				userLog.removeLast();
			if (sender == null)
				userLog.addFirst(LocalDateTime.now().format(DTF) + " " + msg);
			else
				userLog.addFirst(LocalDateTime.now().format(DTF) + " [" + InvUtil.padLeft(sender.getId(), 10) + "] " + msg);
		}
	}

	public List<String> copyUserLog(List<String> out) {
		synchronized (userLog) {
			out.addAll(userLog);
		}
		return out;
	}

}
