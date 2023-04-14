package uk.co.stikman.invmon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import uk.co.stikman.invmon.server.InvMonHTTPResponse;

public class EmbeddedServer extends NanoHTTPD {

	private final HTTPServicer servicer;

	public EmbeddedServer(int port, HTTPServicer servicer) {
		super(port);
		this.servicer = servicer;
	}

	private class HTTPRequestImpl implements InvMonHTTPRequest {

		private IHTTPSession session;

		public HTTPRequestImpl(IHTTPSession session) {
			this.session = session;
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