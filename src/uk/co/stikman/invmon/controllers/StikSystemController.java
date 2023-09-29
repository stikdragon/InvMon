package uk.co.stikman.invmon.controllers;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.inverter.util.InvUtil;

public class StikSystemController extends SimpleInverterController {
	private LocalTime			start;
	private LocalTime			end;
	private boolean				enabled	= false;
	private DateTimeFormatter	dtf;

	public StikSystemController(String id, Env env) {
		super(id, env);
	}

	@Override
	protected void run() throws InvMonException {
		run(LocalDateTime.now());
	}

	private void run(LocalDateTime now) throws InvMonException {
		//
		// if we're in the window, and we're enabled, then switch to charge mode
		//
		if (enabled) {
			List<TimeWindow> windows = new ArrayList<>();
			windows.add(new TimeWindow(now.toLocalDate().plusDays(-1), start, end));
			windows.add(new TimeWindow(now.toLocalDate().plusDays(0), start, end));
			windows.add(new TimeWindow(now.toLocalDate().plusDays(1), start, end));

			TimeWindow in = null;
			for (TimeWindow w : windows) {
				if (w.contains(now))
					in = w;
			}

			if (in != null) {
				setCharging(State.CHARGING, "");
			} else {
				setCharging(State.NOT_CHARGING, "");
			}

		} else { // !enabled
			setCharging(State.NOT_CHARGING, "");
		}
	}

	@Override
	public void configure(Element root) throws InvMonException {
		super.configure(root);
		dtf = DateTimeFormatter.ofPattern("HH:mm");
		start = LocalTime.parse(InvUtil.getAttrib(root, "startTime"), dtf);
		end = LocalTime.parse(InvUtil.getAttrib(root, "endTime"), dtf);
	}

	@Override
	public String toString() {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("  Time: ").append(dtf.format(LocalTime.now())).append("\n");
			sb.append(" State: ").append(getCurrentState()).append("\n");
			return sb.toString();
		} catch (Exception e) {
			return e.toString();
		}
	}

	@Override
	public void start() {
		userLog("Stik's control logic initialised");
	}
}
