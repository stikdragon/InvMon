package uk.co.stikman.invmon.server;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.Response.Status;

public class InvMonHTTPResponse {
	private final long					size;
	private final String				content;
	private final String				mime;
	private final Status				status;
	private final Map<String, String>	headers	= new HashMap<>();
	private final InputStream			stream;

	private InvMonHTTPResponse(Status status, String mime, String content, InputStream stream, long size) {
		this.content = content;
		this.mime = mime;
		this.status = status;
		this.stream = stream;
		this.size = size;
	}

	public InvMonHTTPResponse(Status status, String mime, String content) {
		this(status, mime, content, null, -1);
	}

	public InvMonHTTPResponse(String content) {
		this(Status.OK, "text/html", content);
	}

	public InvMonHTTPResponse(Status status, String mime, InputStream content, long size) {
		this(status, mime, null, content, size);
	}

	public void addHeader(String key, String val) {
		this.headers.put(key, val);
	}

	public long getSize() {
		return size;
	}

	public String getContent() {
		return content;
	}

	public String getMime() {
		return mime;
	}

	public Status getStatus() {
		return status;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public InputStream getStream() {
		return stream;
	}

}
