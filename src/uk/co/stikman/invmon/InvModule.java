package uk.co.stikman.invmon;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.server.Console;
import uk.co.stikman.invmon.server.InvMonClientError;

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

	public void userLog(String msg) {
		if (env != null)
			env.userLog(this, msg);
		else
			System.out.println(msg); // to work during tests 
	}

	public ConsoleResponse consoleCommand(Console console, String cmd) {
		if (cmd.equals("exit")) {
			console.setActiveModule(null);
			return new ConsoleResponse(ConsoleResponseStatus.OK, "Selected module: -none-");
		}
		throw new InvMonClientError("Unknown command: " + cmd);
	}

}
