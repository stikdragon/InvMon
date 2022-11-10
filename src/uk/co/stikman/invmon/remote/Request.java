package uk.co.stikman.invmon.remote;

public class Request {

	private byte[] data;
	private ServerSession sesh;

	public Request(byte[] data, ServerSession sesh) {
		this.data = data;
		this.sesh = sesh;
	}

	public byte[] getData() {
		return data;
	}

	public ServerSession getSession() {
		return sesh;
	}

}
