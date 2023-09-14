package uk.co.stikman.invmon.server;

public class AuthedSession {
	private final User		user;
	private final String	uid;

	public AuthedSession(User user, String uid) {
		super();
		this.user = user;
		this.uid = uid;
	}

	public User getUser() {
		return user;
	}

	public String getUid() {
		return uid;
	}
}
