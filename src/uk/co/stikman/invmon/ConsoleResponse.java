package uk.co.stikman.invmon;

public class ConsoleResponse {
	private String						text;
	private final ConsoleResponseStatus	status;
	private boolean						formatted	= false;
	private InvModule					activeModule;

	public ConsoleResponse(ConsoleResponseStatus status, String text, boolean formatted) {
		this.status = status;
		this.text = text;
		this.formatted = formatted;
	}

	public ConsoleResponse(String text) {
		this(text, false);
	}

	public ConsoleResponse(String text, boolean formatted) {
		this.status = ConsoleResponseStatus.OK;
		this.text = text;
		this.formatted = formatted;
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

	public boolean isFormatted() {
		return formatted;
	}

	public void setFormatted(boolean formatted) {
		this.formatted = formatted;
	}

}
