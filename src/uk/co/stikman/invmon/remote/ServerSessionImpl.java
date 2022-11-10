package uk.co.stikman.invmon.remote;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

class ServerSessionImpl implements ServerSession {

	private SocketAddress				remoteAddress;
	private final Map<String, Object>	userObj	= new HashMap<>();

	void setRemoteAddress(SocketAddress remote) {
		this.remoteAddress = remote;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	@Override
	public void put(String key, Object val) {
		userObj.put(key, val);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(String key) {
		return (T) userObj.get(key);
	}

}
