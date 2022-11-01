package uk.co.stikman.invmon;

import org.w3c.dom.Element;

/**
 * contructor must be <code>(Env env, String id)</code>
 * 
 * @author stikd
 *
 */
public interface ProcessPart {

	String getId();
	
	void configure(Element config);

	void start() throws InvMonException;
	void terminate();



}
