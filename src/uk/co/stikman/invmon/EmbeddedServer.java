package uk.co.stikman.invmon;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.co.stikman.invmon.nanohttpd.NanoHTTPD;
import uk.co.stikman.invmon.server.InvMonHTTPResponse;

public class EmbeddedServer extends NanoHTTPD {

	private final HTTPServicer servicer;

	public EmbeddedServer(int port, HTTPServicer servicer) {
		super(port);
		this.servicer = servicer;
	}

	private class HTTPRequestImpl implements InvMonHTTPRequest {

		private IHTTPSession	session;
		Map<String, String>		bodyData;

		public HTTPRequestImpl(IHTTPSession session) {
			this.session = session;
			if (session.getMethod() == Method.POST) {
				//
				// read body, otherwise there's a bug in nanohttpd 
				//  https://github.com/NanoHttpd/nanohttpd/issues/356
				//
				bodyData = new HashMap<>();
				try {
					session.parseBody(bodyData);
				} catch (IOException | ResponseException e) {
					throw new RuntimeException("POST parseBody failed: " + e.getMessage(), e);
				}
			}
		}

		@Override
		public boolean isMethod(String name) {
			return name.equals(getMethod());
		}

		@Override
		public String getMethod() {
			return session.getMethod().name();
		}

		@Override
		public String getUri() {
			return session.getUri();
		}

		@Override
		public Map<String, String> getCookies() {
			Map<String, String> x = new HashMap<>();
			session.getCookies().forEach(k -> x.put(k, session.getCookies().read(k)));
			return x;
		}

		@Override
		public Map<String, List<String>> getParameters() {
			return session.getParameters();
		}

		@Override
		public String getQueryParameterString() {
			return session.getQueryParameterString();
		}

		@Override
		public String getBodyAsString() {
			if (session.getMethod() != Method.POST)
				throw new IllegalStateException("Only supported for POST requests");
			return bodyData.get("postData");
		}

		@Override
		public String optParam(String name, String def) {
			List<String> lst = getParameters().get(name);
			if (lst == null || lst.isEmpty())
				return def;
			return lst.get(0);
		}
	}

	@Override
	public Response serve(IHTTPSession session) {
		HTTPRequestImpl x = new HTTPRequestImpl(session);
		InvMonHTTPResponse res = servicer.serve(x);
		Response out;
		if (res.getStream() == null)
			out = NanoHTTPD.newFixedLengthResponse(res.getStatus(), res.getMime(), res.getContent());
		else
			out = NanoHTTPD.newFixedLengthResponse(res.getStatus(), res.getMime(), res.getStream(), res.getSize());
		res.getHeaders().entrySet().forEach(e -> out.addHeader(e.getKey(), e.getValue()));
		return out;
	}

}