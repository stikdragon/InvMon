package uk.co.stikman.invmon;

public class InvMonException extends Exception {

	public InvMonException(Throwable cause) {
		super(cause);
	}

	public InvMonException() {
		super();
	}

	public InvMonException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvMonException(String message) {
		super(message);
	}

}
