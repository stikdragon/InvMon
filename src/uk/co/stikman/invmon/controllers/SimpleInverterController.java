package uk.co.stikman.invmon.controllers;

import java.util.List;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.ConsoleHelpInfo;
import uk.co.stikman.invmon.ConsoleResponse;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.InverterMonitor;
import uk.co.stikman.invmon.inverter.PIP8048MAX.OutputMode;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.server.Console;
import uk.co.stikman.invmon.server.UserRole;
import uk.co.stikman.log.StikLog;

public abstract class SimpleInverterController extends InverterController {
	private static final StikLog LOGGER = StikLog.getLogger(SimpleInverterController.class);

	public enum State {
		UNKNOWN,
		NOT_CHARGING,
		CHARGING
	}

	private State	currentState	= State.UNKNOWN;
	private String	inverterId;

	public SimpleInverterController(String id, Env env) {
		super(id, env);
	}

	@Override
	public void configure(Element root) throws InvMonException {
		inverterId = InvUtil.getAttrib(root, "inverter", null);
	}

	protected synchronized void setCharging(State state, String hint) throws InvMonException {
		if (currentState == state) // no change
			return;

		LOGGER.debug("setCharging: " + state + " (\"" + hint + "\")");

		//
		// send to inverter
		//
		if (inverterId != null) {
			InverterMonitor inv = getEnv().findModule(inverterId);
			if (inv == null)
				throw new InvMonException("Controller target inverter [" + inverterId + "] not found");
			inv.setOutputMode(state == State.CHARGING ? OutputMode.UTIL_SOL_BAT : OutputMode.SOL_BAT_UTIL);
		}

		userLog("Charge State changed to: [" + state.name() + "]. (" + hint + ")");
		currentState = state;
	}

	public String getInverterId() {
		return inverterId;
	}

	public State getCurrentState() {
		return currentState;
	}

	@Override
	public ConsoleResponse consoleCommand(Console console, String cmd) throws InvMonException {
		if (cmd.equals("set charge on") || cmd.equals("set charge off")) {
			console.getSession().requireUserRole(UserRole.ADMIN);
			setCharging(cmd.endsWith("off") ? State.NOT_CHARGING : State.CHARGING, "forced by console user");
			return new ConsoleResponse("ok");
		} else
			return super.consoleCommand(console, cmd);
	}

	@Override
	protected void populateCommandHelp(List<ConsoleHelpInfo> lst) {
		super.populateCommandHelp(lst);
		lst.add(new ConsoleHelpInfo("set charge on", "force inverter charge state on"));
		lst.add(new ConsoleHelpInfo("set charge off", "force inverter charge state off"));
	}

}
