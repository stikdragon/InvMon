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

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.InverterMonitor;
import uk.co.stikman.invmon.inverter.PIP8048MAX.OutputMode;
import uk.co.stikman.invmon.inverter.util.FileBackedDataTable;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.inverter.util.Mutable;
import uk.co.stikman.table.CSVImporter;
import uk.co.stikman.table.DataRecord;

/**
 * <p>
 * Red's requirements were:
 * <p>
 * "are you able to add a grid charging feature to invmon? i think i know what i
 * want it to do, during a time window charge up to a soc percentage from a csv
 * and once hit not charge again untill the next evening, the csv will just be
 * 366 percentages so different seasons can have different levels!"
 * 
 * @author stikd
 *
 */
public class RedControllerLogic implements ControllerLogic {

	private enum State {
		NOT_CHARGING,
		CHARGING
	}

	private FileBackedDataTable	csv;
	private InverterController	owner;
	private String				inverterId;
	private State				currentState		= State.NOT_CHARGING;
	private DateTimeFormatter	dtf;
	private Set<TimeWindow>		completedWindows	= new HashSet<>();
	private LocalTime			start;
	private LocalTime			end;

	public RedControllerLogic(InverterController owner) {
		this.owner = owner;
	}

	@Override
	public void config(Element root) throws InvMonException {
		File file = new File(InvUtil.getAttrib(root, "csv"));
		if (!file.exists())
			throw new InvMonException("CSV file [" + file.toString() + "] does not exist");
		csv = new FileBackedDataTable(file);
		csv.setImporter(new CSVImporter());
		csv.setOnReload(dt -> dt.getTable().createIndex("day"));

		dtf = DateTimeFormatter.ofPattern("HH:mm");
		start = LocalTime.parse(InvUtil.getAttrib(root, "startTime"), dtf);
		end = LocalTime.parse(InvUtil.getAttrib(root, "endTime"), dtf);
		inverterId = InvUtil.getAttrib(root, "inverter");
	}

	@Override
	public void run() throws InvMonException {
		State s = run(LocalDateTime.now());
		if (s != null)
			setCharging(s);
	}

	public State run(LocalDateTime now) throws InvMonException {
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

		//
		// remove any that are complete
		//
		windows.removeAll(completedWindows);

		Mutable<State> outcome = new Mutable<>();
		//
		// see if we're in any window at the moment, if we're not then we must 
		// never be charging
		//
		TimeWindow in = null;
		for (TimeWindow w : windows) {
			if (w.contains(now))
				in = w;
		}

		boolean inWindow = in != null;
		if (!inWindow)
			return State.NOT_CHARGING;

		//
		// get today's threshold
		//
		int soc = 25;
		int threshold = getTodayThreshold(getCurrentDayNumber(now.toLocalDate()));

		//
		// if we're in the time window, we're not charging, and we're under the target SoC
		// then go into charge mode
		//
		if (inWindow && currentState == State.NOT_CHARGING && soc < threshold) {
			outcome.set(State.CHARGING);
		}

		if (inWindow && currentState == State.CHARGING && soc >= threshold) {
			outcome.set(State.NOT_CHARGING);
			completedWindows.add(in);
		}

		return outcome.get();
	}

	private int getTodayThreshold(int today) throws InvMonException {
		DataRecord rec = csv.getTable().findRecord("day", Integer.toString(today));
		if (rec == null)
			throw new InvMonException("Day [" + today + "] does not have a row in the CSV file");
		return rec.getInt("soc");
	}

	private void setCharging(State state) throws InvMonException {
		if (currentState == state) // no change
			return;

		//
		// send to inverter
		//
		InverterMonitor inv = owner.getEnv().findModule(inverterId);
		if (inv == null)
			throw new InvMonException("Controller target inverter [" + inverterId + "] not found");
		inv.setOutputMode(state == State.CHARGING ? OutputMode.UTIL_SOL_BAT : OutputMode.SOL_BAT_UTIL);
		currentState = state;
	}

	private static int getCurrentDayNumber(LocalDate now) {
		return now.getDayOfYear();
	}

	@Override
	public String toString() {
		try {
			int today = getCurrentDayNumber(LocalDate.now());
			StringBuilder sb = new StringBuilder();
			sb.append("  Time: ").append(dtf.format(LocalTime.now())).append("\n");
			sb.append(" State: ").append(currentState).append("\n");
			sb.append(" Today: ").append(today).append("\n");
			sb.append("Target: ").append(getTodayThreshold(today)).append("%\n");
			return sb.toString();
		} catch (Exception e) {
			return e.toString();
		}
	}

	public LocalTime getStart() {
		return start;
	}

	public void setStart(LocalTime start) {
		this.start = start;
	}

	public LocalTime getEnd() {
		return end;
	}

	public void setEnd(LocalTime end) {
		this.end = end;
	}

}
