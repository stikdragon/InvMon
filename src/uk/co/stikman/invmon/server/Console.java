package uk.co.stikman.invmon.server;

import uk.co.stikman.invmon.ConsoleResponse;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.table.DataTable;
import uk.co.stikman.utils.Utils;

public class Console {

	private Env			env;
	private InvModule	activeModule;
	private UserSesh sesh;

	public Console(Env env, UserSesh sesh) {
		this.env = env;
		this.sesh = sesh;
	}

	/**
	 * command can never be null
	 * @param sesh 
	 * 
	 * @param command
	 * @return
	 * @throws InvMonException 
	 */
	public ConsoleResponse execute(String command) throws InvMonException {
		ConsoleResponse res;
		if (activeModule == null)
			res = runNoActive(command);
		else
			res = activeModule.consoleCommand(this, command);
		res.setActiveModule(activeModule);
		return res;
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
			DataTable dt = new DataTable();
			dt.addFields("id", "class");
			for (InvModule m : env.getModules()) 
				dt.addRecord(m.getId(), m.getClass().getSimpleName());
			return new ConsoleResponse("Available modules: \n\n" + dt.toString());
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

	public UserSesh getSession() {
		return sesh;
	}

}
