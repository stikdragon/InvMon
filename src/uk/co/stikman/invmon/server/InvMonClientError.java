package uk.co.stikman.invmon.server;

/**
 * these exceptions are considered safe to send back to the client. they won't
 * have their cause sent back, but their message is. note that if you use
 * {@link #InvMonClientError(Throwable)} then it'll get a generic "Internal
 * Error" exception message, rather than copying the cause
 * 
 * @author stik
 *
 */
public class InvMonClientError extends RuntimeException {

	public InvMonClientError(String message, Throwable cause) {
		super(message, cause);
	}

	public InvMonClientError(String message) {
		super(message);
	}

	public InvMonClientError(Throwable cause) {
		super("Internal Error", cause);
	}

	private static final long serialVersionUID = 1L;

}
