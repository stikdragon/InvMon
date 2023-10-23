package uk.co.stikman.invmon;

public class ConsoleResponse {
	private String						text;
	private final ConsoleResponseStatus	status;
	private InvModule					activeModule;

	public ConsoleResponse(ConsoleResponseStatus status, String text) {
		this.status = status;
		this.text = text;
	}

	public ConsoleResponse(String text) {
		this.status = ConsoleResponseStatus.OK;
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public ConsoleResponseStatus getStatus() {
		return status;
	}

	public InvModule getActiveModule() {
		return activeModule;
	}

	public void setActiveModule(InvModule activeModule) {
		this.activeModule = activeModule;
	}

}
