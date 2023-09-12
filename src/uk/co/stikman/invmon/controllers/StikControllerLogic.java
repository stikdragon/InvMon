package uk.co.stikman.invmon.controllers;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvMonException;

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

}
