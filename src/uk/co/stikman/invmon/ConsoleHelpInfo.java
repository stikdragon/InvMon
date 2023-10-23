package uk.co.stikman.invmon;

public class ConsoleHelpInfo {
	private String	command;
	private String	detail;

	public ConsoleHelpInfo(String command, String detail) {
		super();
		this.command = command;
		this.detail = detail;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

}
