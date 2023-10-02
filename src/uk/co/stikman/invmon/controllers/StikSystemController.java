package uk.co.stikman.invmon.controllers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.inverter.util.InvUtil;

public class StikSystemController extends SimpleInverterController {
	private LocalTime			start;
	private LocalTime			end;
	private boolean				enabled			= false;
	private DateTimeFormatter	dtf;
	private int					boostRemaining	= 0;
	private long				lastTS			= -1;

	public StikSystemController(String id, Env env) {
		super(id, env);
	}

	@Override
	protected void run() throws InvMonException {
		run(LocalDateTime.now());
	}

	private void run(LocalDateTime now) throws InvMonException {
		//
		// if we're in the window, and there's some boost minutes remaining then
		// switch to charge mode
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

		System.out.println(new Date().toString() + " Run.  boost==" + boostRemaining);

		long ts = now.toEpochSecond(ZoneOffset.UTC);
		if (in != null) { // in a window, so do the boost stuff
			if (boostRemaining > 0) {
				long dt = 0;
				if (lastTS != -1)
					dt = ts - lastTS;

				dt /= 60; // minutes
				boostRemaining -= dt;
				if (boostRemaining < 0)
					boostRemaining = 0;

				setCharging(State.CHARGING, "In window, boost=" + boostRemaining);
			} else {
				setCharging(State.NOT_CHARGING, "");
			}

		} else {
			setCharging(State.NOT_CHARGING, "");
		}
		lastTS = ts;
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
			sb.append(" Boost: ");
			if (boostRemaining > 0)
				sb.append(boostRemaining).append(" min");
			else
				sb.append("-");
			sb.append("\n");
			return sb.toString();
		} catch (Exception e) {
			return e.toString();
		}
	}

	public void setBoost(int minutes) {
		userLog("Boosting for " + minutes + " minutes (within window)");
		boostRemaining = minutes;
	}

	@Override
	public void start() throws InvMonException {
		super.start();
		userLog("Stik's control logic initialised");
	}
}
