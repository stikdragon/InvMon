package uk.co.stikman.invmon.remote;

import java.net.SocketAddress;

public interface ServerSession {

	SocketAddress getRemoteAddress();

	void put(String key, Object val);

	<T> T get(String key);

}
