package uk.co.stikman.invmon.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class UserSesh {
	private static final long	EXPIRY_TIME	= 60 * 60 * 1000;
	private static final Random	rng			= new Random();
	private final String		id;
	private long				lastTouched;
	private Map<String, Object>	data		= new HashMap<>();
	private AuthedSession		authedSession;

	public UserSesh() {
		super();
		this.id = Long.toString(System.currentTimeMillis() ^ rng.nextLong(), 36);
		touch();
	}

	public void touch() {
		lastTouched = System.currentTimeMillis();
	}

	public String getId() {
		return id;
	}

	public boolean hasExpired() {
		long dt = System.currentTimeMillis() - lastTouched;
		return dt > EXPIRY_TIME;
	}

	@SuppressWarnings("unchecked")
	public <T> T getData(String key) {
		return (T) data.get(key);
	}

	public void putData(String key, Object o) {
		data.put(key, o);
	}

	@Override
	public String toString() {
		return id;
	}

	public void setAuthedUserSession(AuthedSession as) {
		this.authedSession = as;
	}

	public AuthedSession getAuthedUserSession() {
		return authedSession;
	}

	/**
	 * makes sure there is a user logged in, and they have the given role
	 * 
	 * @param role
	 */
	public void requireUserRole(UserRole role) {
		if (authedSession == null)
			throw new InvMonClientError("User not logged in");
		if (authedSession.getUser().getRole() != role)
			throw new InvMonClientError("User not authorised");
	}

}
