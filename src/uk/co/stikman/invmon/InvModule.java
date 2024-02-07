package uk.co.stikman.invmon;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.server.Console;
import uk.co.stikman.invmon.server.InvMonClientError;
import uk.co.stikman.utils.Utils;

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

	public ConsoleResponse consoleCommand(Console console, String cmd) throws InvMonException {
		if (cmd.equals("help")) {
			List<ConsoleHelpInfo> lst = new ArrayList<>();
			populateCommandHelp(lst);
			lst.sort((a, b) -> a.getCommand().compareTo(b.getCommand()));

			int max = 0;
			for (ConsoleHelpInfo x : lst)
				max = Math.max(max, x.getCommand().length());
			max += 4;

			StringBuilder sb = new StringBuilder();
			for (ConsoleHelpInfo x : lst) {
				String[] lines = x.getDetail().split("\n");
				for (int i = 0; i < lines.length; ++i)
					sb.append("^5").append(Utils.padString(i == 0 ? x.getCommand() : "", ' ', max)).append("^x").append(i == 0 ? "- " : "  ").append(lines[i]).append("\n");
			}
			return new ConsoleResponse(sb.toString(), true);
		}

		if (cmd.equals("summary"))
			return new ConsoleResponse(toString());

		if (cmd.equals("exit")) {
			console.setActiveModule(null);
			return new ConsoleResponse(ConsoleResponseStatus.OK, "Selected module: -none-", false);
		}
		throw new InvMonClientError("Unknown command: " + cmd);
	}

	protected void populateCommandHelp(List<ConsoleHelpInfo> lst) {
		lst.add(new ConsoleHelpInfo("exit", "Return to root"));
		lst.add(new ConsoleHelpInfo("help", "Show this"));
		lst.add(new ConsoleHelpInfo("summary", "Summarise the current module"));
	}

}
