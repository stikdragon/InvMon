package uk.co.stikman.invmon.htmlout;

import java.io.IOException;
import java.util.List;

import org.w3c.dom.Element;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.inverter.InvUtil;
import uk.co.stikman.log.StikLog;

public class HTTPServer extends InvModule {

	private static final StikLog	LOGGER	= StikLog.getLogger(HTTPServer.class);
	private DataLogger				datalogger;
	private int						port;
	private Svr						svr;

	public HTTPServer(String id, Env env) {
		super(id, env);
	}

	@Override
	public void configure(Element config) {
		this.port = Integer.parseInt(InvUtil.getAttrib(config, "port"));
	}

	@Override
	public void start() throws InvMonException {
		super.start();
		this.datalogger = getEnv().getModule("datalogger");
		svr = new Svr(port, this);
		try {
			svr.start();
		} catch (IOException e) {
			throw new InvMonException("Could not start NanoHTTPD: " + e.getMessage(), e);
		}
	}

	@Override
	public void terminate() {
		svr.stop();
		super.terminate();
	}

	private Response serve(IHTTPSession session) {
		try {
			String offset = getParam(session, "off");
			String duration = getParam(session, "dur");

			HTMLOpts opts = new HTMLOpts();
			opts.setDuration(duration == null ? 60 * 10 : Long.parseLong(duration));
			opts.setOffset(offset == null ? 0 : Long.parseLong(offset));

			HTMLBuilder html = new HTMLBuilder();
			new HTMLGenerator(datalogger).render(html, opts);

			return NanoHTTPD.newFixedLengthResponse(Status.OK, "text/html", html.toString());
		} catch (Exception e) {
			LOGGER.error(e);
			return NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, "text/html", "Internal Error");
		}
	}

	private static String getParam(IHTTPSession session, String key) {
		List<String> lst = session.getParameters().get(key);
		if (lst == null)
			return null;
		return lst.get(0);
	}

	public static class Svr extends NanoHTTPD {

		private HTTPServer owner;

		@Override
		public Response serve(IHTTPSession session) {
			return owner.serve(session);
		}

		public Svr(int port, HTTPServer owner) {
			super(port);
			this.owner = owner;
		}

	}

}
