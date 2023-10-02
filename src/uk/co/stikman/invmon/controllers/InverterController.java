package uk.co.stikman.invmon.controllers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.log.StikLog;

public abstract class InverterController extends InvModule {
	private static final StikLog	LOGGER		= StikLog.getLogger(InverterController.class);
	private File					file;
	private Map<String, String>		properties	= new HashMap<>();
	private long					lastUpdate;

	public InverterController(String id, Env env) {
		super(id, env);
	}

	@Subscribe(Events.TIMER_UPDATE_MINUTE)
	public void timer() {
		//
		// fires every minute
		//
		try {
			run();
		} catch (InvMonException e) {
			LOGGER.error(e);
		}
	}

	protected void run() throws InvMonException {

	}

}
