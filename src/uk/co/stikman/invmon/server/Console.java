package uk.co.stikman.invmon.server;

import java.util.NoSuchElementException;

import uk.co.stikman.invmon.ConsoleResponse;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.utils.Utils;

public class Console {

	private Env			env;
	private InvModule	activeModule;

	public Console(Env env) {
		this.env = env;
	}

	/**
	 * command can never be null
	 * 
	 * @param command
	 * @return
	 */
	public ConsoleResponse execute(String command) {
		if (activeModule == null) {
			return runNoActive(command);
		} else {
			return activeModule.consoleCommand(this, command);
		}
	}

	private ConsoleResponse runNoActive(String command) {
		if (command.equals("help")) {
			return new ConsoleResponse("""
					help             - show this
					select <module>  - switch to a module
					list modules     - show all available modules""");
		} else if (command.equals("list modules")) {
			StringBuilder sb = new StringBuilder();
			sb.append("Available modules:\n\n");
			for (InvModule m : env.getModules()) {
				sb.append("   ").append(Utils.padString(m.getId(), ' ', 20)).append(": ");
				sb.append(m.getClass().getSimpleName()).append("\n");
			}
			return new ConsoleResponse(sb.toString());
		} else if (command.startsWith("select")) {
			String[] bits = command.split(" ");
			if (bits.length != 2)
				throw new InvMonClientError("Invalid command, expected one argument");
			setActiveModule(env.getModule(bits[1]));
			return new ConsoleResponse("Selected module: " + bits[1] + "    (\"exit\" to return to root)");
		} else
			throw new InvMonClientError("Unknown command: " + command);
	}

	public InvModule getActiveModule() {
		return activeModule;
	}

	public void setActiveModule(InvModule activeModule) {
		this.activeModule = activeModule;
	}

}
