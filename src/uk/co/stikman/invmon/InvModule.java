package uk.co.stikman.invmon;

import org.w3c.dom.Element;

/**
 * A component in the inverter monitor system
 * 
 * contructor must be <code>(String id, Env env)</code>
 * 
 * @author stikd
 *
 */
public abstract class InvModule {

	private final Env		env;
	private final String	id;

	public InvModule(String id, Env env) {
		super();
		this.id = id;
		this.env = env;
	}

	public String getId() {
		return id;
	}

	public abstract void configure(Element config) throws InvMonException;

	public Env getEnv() {
		return env;
	}

	public void start() throws InvMonException {
		getEnv().getBus().register(this);
	}

	public void terminate() {
		getEnv().getBus().unregister(this);
	};

}
