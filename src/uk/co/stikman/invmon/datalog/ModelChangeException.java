package uk.co.stikman.invmon.datalog;

public class ModelChangeException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4156967923828804089L;

	public ModelChangeException(String message, Throwable cause) {
		super(message, cause);
	}

	public ModelChangeException(String message) {
		super(message);
	}

	public ModelChangeException(Throwable cause) {
		super(cause);
	}

}
