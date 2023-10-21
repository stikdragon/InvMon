package uk.co.stikman.invmon.controllers;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.InverterMonitor;
import uk.co.stikman.invmon.inverter.PIP8048MAX.OutputMode;
import uk.co.stikman.invmon.inverter.util.InvUtil;

public abstract class SimpleInverterController extends InverterController {
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

	protected void setCharging(State state, String hint) throws InvMonException {
		if (currentState == state) // no change
			return;

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

}
