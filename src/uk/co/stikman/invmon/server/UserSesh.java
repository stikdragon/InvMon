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
	private HTTPServer			owner;

	public UserSesh(HTTPServer httpServer) {
		super();
		this.id = Long.toString(System.currentTimeMillis() ^ rng.nextLong(), 36);
		this.owner = httpServer;
		touch();
	}

	public HTTPServer getOwner() {
		return owner;
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
		if (getOwner().getSecurityMode() == SecurityMode.ALLOW_ALL)
			return;
		UserRole r = UserRole.PUBLIC;
		if (authedSession != null)
			r = authedSession.getUser().getRole();
		if (r.getLevel() < role.getLevel())
			throw new InvMonClientError("User not authorised");
	}

}
