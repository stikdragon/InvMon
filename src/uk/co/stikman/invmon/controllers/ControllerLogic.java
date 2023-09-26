package uk.co.stikman.invmon.controllers;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.PollData;

public interface ControllerLogic {

	void config(Element root) throws InvMonException;

	void run() throws InvMonException;

	void acceptPollData(PollData data);

}
