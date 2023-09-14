package uk.co.stikman.invmon.controllers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.log.StikLog;

public class InverterController extends InvModule {
	private static final StikLog	LOGGER		= StikLog.getLogger(InverterController.class);
	private File					file;
	private Map<String, String>		properties	= new HashMap<>();
	private long					lastUpdate;
	private ControllerLogic			logic;

	public InverterController(String id, Env env) {
		super(id, env);
	}

	@Override
	public void configure(Element root) throws InvMonException {
		String s = InvUtil.getAttrib(root, "type");
		if ("red_csv".equals(s)) {
			logic = new RedControllerLogic(this);
		} else if ("stik1".equals(s)) {
			logic = new StikControllerLogic(this);
		} else
			throw new InvMonException("<InverterController> type attribute is not recognised: " + s);

		logic.config(root);
	}

	@Subscribe(Events.TIMER_UPDATE_MINUTE)
	public void timer() {
		//
		// fires every minute
		//
		try {
			logic.run();
		} catch (InvMonException e) {
			LOGGER.error(e);
		}
	}

	@Override
	public String toString() {
		return logic.toString();
	}
}
