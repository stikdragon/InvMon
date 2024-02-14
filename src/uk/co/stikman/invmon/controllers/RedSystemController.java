package uk.co.stikman.invmon.controllers;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.controllers.SolarKickerConfig.Channel;
import uk.co.stikman.invmon.inverter.util.FileBackedDataTable;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.log.StikLog;
import uk.co.stikman.table.CSVImporter;
import uk.co.stikman.table.DataRecord;

public class RedSystemController extends SimpleInverterController {

	/**
	 * responsible for managing a serial port where we poke the DTR/RTS line high to
	 * drive a contactor
	 * 
	 * @author stik
	 *
	 */
	private class KickerOutputThread extends Thread {

		private boolean	terminated		= false;
		private boolean	rtsTriggered	= false;
		private boolean	dtrTriggered	= false;

		@Override
		public void run() {
			SerialPort port = SerialPort.getCommPort(solarKicker.portName);
			port.openPort(9600);
			port.clearRTS();
			port.clearDTR();
			try {
				for (;;) {
					if (terminated)
						return;

					if (rtsTriggered) {
						rtsTriggered = false;
						port.setRTS();
						try {
							sleep(solarKicker.getContactorTime());
						} catch (InterruptedException e) {
							LOGGER.warn("Interrupted while pulsing RTS");
						}
						port.clearRTS();
					}
					if (terminated)
						return;

					if (dtrTriggered) {
						dtrTriggered = false;
						port.setRTS();
						try {
							sleep(solarKicker.getContactorTime());
						} catch (InterruptedException e) {
							LOGGER.warn("Interrupted while pulsing DTR");
						}
						port.clearRTS();
					}

					if (terminated)
						return;

					try {
						sleep(1000);
					} catch (InterruptedException e) {
					}
				}
			} finally {
				port.closePort();
			}
		}

		public void terminate() {
			this.terminated = true;
			interrupt();
		}

		public void trigger(Channel ch) {
			if (ch.getAction().equals("rts"))
				rtsTriggered = true;
			else if (ch.getAction().equals("dtr"))
				dtrTriggered = true;
			interrupt();
		}

	}

	private static final StikLog	LOGGER					= StikLog.getLogger(RedSystemController.class);
	private FileBackedDataTable		csv;

	private DateTimeFormatter		dtf;
	private Set<TimeWindow>			completedWindows		= new HashSet<>();
	private LocalTime				start;
	private LocalTime				end;
	private int						soc;

	private SolarKickerConfig		solarKicker				= null;											// if null then it's disabled
	private KickerOutputThread		kickerOutputThread;
	private Set<Channel>			triggeredKickerChannels	= new HashSet<>();

	public RedSystemController(String id, Env env) {
		super(id, env);
	}

	@Override
	public void configure(Element root) throws InvMonException {
		super.configure(root);
		File file = new File(InvUtil.getAttrib(root, "csv"));
		if (!file.exists())
			throw new InvMonException("CSV file [" + file.toString() + "] does not exist");
		csv = new FileBackedDataTable(file);
		csv.setImporter(new CSVImporter());
		csv.setOnReload(dt -> dt.getTable().createIndex("day"));

		dtf = DateTimeFormatter.ofPattern("HH:mm");
		start = LocalTime.parse(InvUtil.getAttrib(root, "startTime"), dtf);
		end = LocalTime.parse(InvUtil.getAttrib(root, "endTime"), dtf);

		Element el = InvUtil.getElement(root, "SolarKicker", true);
		if (el != null) {
			solarKicker = new SolarKickerConfig();
			solarKicker.parseConfig(el);
			kickerOutputThread = new KickerOutputThread();
		}
	}

	@Subscribe(Events.POST_DATA)
	public synchronized void poll(PollData data) {
		soc = (int) (100.0f * data.get(getInverterId()).getFloat("soc"));

		//
		// if the solar kicker thing is enabled then we need to get 
		// voltage/current readings for that too and work out if the 
		// channels need activating or not
		//
		if (solarKicker != null) {
			for (Channel ch : solarKicker.getChannels()) {
				String inv = ch.getSource().split("\\.")[0];
				String fld = ch.getSource().split("\\.")[1];
				float v = data.get(inv).getFloat(fld + "V");
				float i = data.get(inv).getFloat(fld + "I");

				if (!ch.satisfiesThreshold(v, i))
					ch.setThreshMetAt(System.currentTimeMillis()); // reset last timer
				long dt = System.currentTimeMillis() - ch.getThreshMetAt();
				if (dt > ch.getThreshTime()) {
					ch.setThreshMetAt(System.currentTimeMillis());
					triggeredKickerChannels.add(ch);
				}
			}
		}
	}

	@Override
	protected void run() throws InvMonException {
		if (solarKicker != null)
			runKicker();
		runWindowedCharge(LocalDateTime.now(), soc);
	}

	private void runKicker() {
		//
		// send any kicker channels that have activated
		//
		triggeredKickerChannels.forEach(ch -> {
			userLog("SolarKicker conditions met for Channel [" + ch + "], triggering...");
			kickerOutputThread.trigger(ch);
		});
		triggeredKickerChannels.clear();
	}

	private int getTodayThreshold(int today) throws InvMonException {
		DataRecord rec = csv.getTable().findRecord("day", Integer.toString(today));
		if (rec == null)
			throw new InvMonException("Day [" + today + "] does not have a row in the CSV file");
		return rec.getInt("soc");
	}

	private static int getCurrentDayNumber(LocalDate now) {
		return now.getDayOfYear();
	}

	//
	// so the logic here should be:
	//
	//  in window   under thresh    state           OUTCOME
	//  ---------   ------------    ------------    -----------------------------
	//  no          no              not_charging     
	//  no          no              charging        set no-charge, mark completed
	//  no          yes             not_charging     
	//  no          yes             charging        set no-charge, mark completed
	//  yes         no              not_charging     
	//  yes         no              charging        set no-charge, mark completed
	//  yes         yes             not_charging    set charge, if not completed
	//  yes         yes             charging         
	//
	public synchronized State runWindowedCharge(LocalDateTime now, int soc) throws InvMonException {
		//
		// see if we're in a time window, then check if we're under the target
		// SoC% and switch on if so.  If we hit the target, mark today as finished
		// and turn off.  if we've already completed a charge within this time
		// window then don't start again
		//
		List<TimeWindow> windows = new ArrayList<>();
		windows.add(new TimeWindow(now.toLocalDate().plusDays(-1), start, end));
		windows.add(new TimeWindow(now.toLocalDate().plusDays(0), start, end));
		windows.add(new TimeWindow(now.toLocalDate().plusDays(1), start, end));

		TimeWindow in = null;
		for (TimeWindow w : windows) {
			if (w.contains(now))
				in = w;
		}

		boolean inWindow = in != null;

		//
		// get today's threshold
		//
		State currentState = getCurrentState();
		int threshold = getTodayThreshold(getCurrentDayNumber(now.toLocalDate()));
		String hint = "thr=" + threshold + "%,  soc=" + soc + "%";
		LOGGER.debug(String.format("Run:wnd==%s, soc=%d, thr=%d, cs=%s", in, soc, threshold, currentState));
		if (!inWindow) {
			if (currentState == State.CHARGING) {
				setCharging(State.NOT_CHARGING, hint);
				completedWindows.add(in);
			}
		} else {
			if (soc > threshold) {
				if (currentState == State.CHARGING) {
					setCharging(State.NOT_CHARGING, hint);
					completedWindows.add(in);
				}
			} else {
				if (currentState != State.CHARGING) {
					if (!completedWindows.contains(in)) {
						setCharging(State.CHARGING, hint);
					}
				}
			}
		}

		return getCurrentState();
	}

	@Override
	public void start() throws InvMonException {
		super.start();
		if (solarKicker != null)
			kickerOutputThread.start();
		userLog("\"Red Logic\" initialised");
	}

	@Override
	public void terminate() {
		if (solarKicker != null)
			kickerOutputThread.terminate();
		super.terminate();
	}

	@Override
	public synchronized String toString() {
		try {
			int today = getCurrentDayNumber(LocalDate.now());
			StringBuilder sb = new StringBuilder();
			sb.append("  Time: ").append(dtf.format(LocalTime.now())).append("\n");
			sb.append(" State: ").append(getCurrentState()).append("\n");
			sb.append(" Today: ").append(today).append("\n");
			sb.append("   SoC: ").append(soc).append("%\n");
			sb.append("Target: ").append(getTodayThreshold(today)).append("%\n");
			return sb.toString();
		} catch (Exception e) {
			return e.toString();
		}
	}

}
