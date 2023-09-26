package uk.co.stikman.invmon.controllers;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.PollData;

public class StikControllerLogic implements ControllerLogic {

	private InverterController owner;

	public StikControllerLogic(InverterController owner) {
		this.owner = owner;
	}

	@Override
	public void config(Element root) {

	}

	@Override
	public void run() throws InvMonException {
		
	}

	@Override
	public void acceptPollData(PollData data) {
		
	}

}
