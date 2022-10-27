package uk.co.stikman.invmon.inverter;

public class CommunicationError extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CommunicationError(String message) {
		super(message);
	}

	public CommunicationError(Throwable cause) {
		super(cause);
	}

	public CommunicationError(String message, Throwable cause) {
		super(message, cause);
	}

}
