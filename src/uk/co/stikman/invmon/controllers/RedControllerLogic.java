package uk.co.stikman.invmon.controllers;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
	private int					lastCompletedDay	= -1;
	private LocalTime			windowStart;
	private LocalTime			windowEnd;
	private String				inverterId;
	private State				currentState		= State.NOT_CHARGING;
	private DateTimeFormatter	dtf;

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
		windowStart = LocalTime.parse(InvUtil.getAttrib(root, "startTime"), dtf);
		windowEnd = LocalTime.parse(InvUtil.getAttrib(root, "endTime"), dtf);
		inverterId = InvUtil.getAttrib(root, "inverter");
	}

	@Override
	public void run() throws InvMonException {
		//
		// see if we're in the time window, then check if we're under the target
		// SoC% and switch on if so.  If we hit the target, mark today as finished
		// and turn off
		//
		int today = getCurrentDayNumber();
		if (lastCompletedDay >= today) // already done it today, so stop immediately
			return;

		Mutable<State> outcome = new Mutable<>();
		try {
			int soc = 25;
			boolean inWindow = isInWindow(LocalTime.now());

			//
			// get today's threshold
			//
			int threshold = getTodayThreshold(today);

			//
			// if we're not in the time window then we must never be charging
			//
			if (!inWindow) {
				outcome.set(State.NOT_CHARGING);
				return;
			}

			//
			// if we're in the time window, we're not charging, and we're under the target SoC
			// then go into charge mode
			//
			if (inWindow && currentState == State.NOT_CHARGING && soc < threshold) {
				outcome.set(State.CHARGING);
			}

			if (inWindow && currentState == State.CHARGING && soc >= threshold) {
				outcome.set(State.NOT_CHARGING);
				lastCompletedDay = today; // prevent us from going back into charge today
			}
		} finally {
			//
			// actually send the instruction to the inverter
			//
			if (outcome.get() != null)
				setCharging(outcome.get());
		}
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

	private boolean isInWindow(LocalTime now) {
		//
		// if our window spans midnight (ie. start>end) then 
		// we need to check in two halves
		//
		if (windowStart.isAfter(windowEnd))
			return (windowStart.isBefore(now) && LocalTime.MIDNIGHT.isAfter(now)) || (LocalTime.MIDNIGHT.isBefore(now) && windowEnd.isAfter(now));
		else
			return windowStart.isBefore(now) && windowEnd.isAfter(now);
	}

	private static int getCurrentDayNumber() {
		return LocalDate.now().getDayOfYear();
	}

	@Override
	public String toString() {
		try {
			int today = getCurrentDayNumber();
			StringBuilder sb = new StringBuilder();
			sb.append("  Time: ").append(dtf.format(LocalTime.now())).append("\n");
			sb.append(" State: ").append(currentState).append("\n");
			sb.append(" Today: ").append(today).append("\n");
			sb.append("Target: ").append(getTodayThreshold(today)).append("%\n");
			sb.append("  Done? ").append(lastCompletedDay >= today ? "Yes" : "-").append("\n");
			return sb.toString();
		} catch (Exception e) {
			return e.toString();
		}
	}

}
