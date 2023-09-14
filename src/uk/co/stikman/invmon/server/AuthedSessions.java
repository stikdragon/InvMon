package uk.co.stikman.invmon.server;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

public class AuthedSessions {

	private Random						rng			= new Random();
	private Map<String, AuthedSession>	sessions	= new HashMap<>();

	public synchronized AuthedSession createNew(User u) {
		String uid = newId();
		AuthedSession x = new AuthedSession(u, uid);
		sessions.put(uid, x);
		return x;
	}
	
	public synchronized AuthedSession getById(String uid) {
		AuthedSession x = sessions.get(uid);
		if (x == null)
			throw new NoSuchElementException(uid);
		return x;
	}

	private String newId() {
		synchronized (rng) {
			String out = "";
			for (int i = 0; i < 20; ++i)
				out += Character.toString(26 + rng.nextInt(26));
			return out;
		}
	}

}
