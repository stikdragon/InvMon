package uk.co.stikman.invmon.htmlout;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.htmlout.res.Res;
import uk.co.stikman.invmon.inverter.InvUtil;
import uk.co.stikman.log.StikLog;

public class HTTPServer extends InvModule {

	private interface FetchMethod {
		Response fetch(String url, IHTTPSession session) throws Exception;
	}

	private static final StikLog			LOGGER		= StikLog.getLogger(HTTPServer.class);
	private DataLogger						datalogger;
	private int								port;
	private Svr								svr;
	private PollData						lastData;
	private final Map<String, FetchMethod>	urlMappings	= new HashMap<>();

	public HTTPServer(String id, Env env) {
		super(id, env);
		urlMappings.put("old.html", this::staticPage);
		urlMappings.put("background.png", this::page);
		urlMappings.put("style.css", this::page);
		urlMappings.put("index.html", this::page);
		urlMappings.put("main.js", this::page);
		urlMappings.put("util.js", this::page);

		urlMappings.put("query", this::query);
		urlMappings.put("config", this::getPageConfig);
	}

	/**
	 * just a file without anything else
	 * 
	 * @param session
	 * @return
	 */
	private Response page(String url, IHTTPSession session) throws Exception {
		Res r = Res.get(url);
		return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.getMimeTypeForFile(url), r.makeStream(), r.getSize());
	}

	/**
	 * the original static page logic
	 * 
	 * @param session
	 * @return
	 * @throws NotFoundException
	 */
	private Response staticPage(String url, IHTTPSession session) throws Exception {
		String offset = getParam(session, "off");
		String duration = getParam(session, "dur");

		HTMLOpts opts = new HTMLOpts();
		opts.setDuration(duration == null ? 60 * 10 : Long.parseLong(duration));
		opts.setOffset(offset == null ? 0 : Long.parseLong(offset));

		HTMLBuilder html = new HTMLBuilder();
		new HTMLGenerator(datalogger).render(html, opts, lastData);
		return NanoHTTPD.newFixedLengthResponse(Status.OK, "text/html", html.toString());
	}

	private Response query(String url, IHTTPSession session) throws Exception {
		return NanoHTTPD.newFixedLengthResponse(Status.OK, "text/html", "OK");
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

	@Subscribe(Events.POST_DATA)
	public void postData(PollData data) {
		this.lastData = data;
	}

	private Response serve(IHTTPSession session) {
		try {
			String url = session.getUri();
			if (url.equals("/"))
				url = "/index.html";
			url = url.substring(1);

			FetchMethod m = urlMappings.get(url);
			if (m == null)
				throw new NotFoundException("Not found");
			return m.fetch(url, session);

		} catch (NotFoundException nfe) {
			return NanoHTTPD.newFixedLengthResponse(Status.NOT_FOUND, "text/html", "404 Not Found: " + nfe.getMessage());
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

	private Response getPageConfig(String url, IHTTPSession request) {
		String name = InvUtil.getParam(request, "layout");
		if (name == null)
			name = "default";

		// TODO: load layouts

		JSONObject root = new JSONObject();
		root.put("gridSize", 100);
		JSONArray arr = new JSONArray();
		root.put("widgets", arr);
		
		JSONObject wij = new JSONObject();
		wij.put("name", "PV Power");
		wij.put("x", 0).put("y", 0).put("w", 8).put("h", 3);
		wij.put("id", "pvChart").put("type", "pvChart");
		arr.put(wij);
		
		wij = new JSONObject();
		wij.put("name", "PV Power");
		wij.put("x", 8).put("y", 0).put("w", 2).put("h", 3);
		wij.put("id", "pvTable").put("type", "pvTable");
		arr.put(wij);
		
		wij = new JSONObject();
		wij.put("name", "Load");
		wij.put("x", 0).put("y", 3).put("w", 8).put("h", 3);
		wij.put("id", "loadChart").put("type", "loadChart");
		arr.put(wij);
		
		wij = new JSONObject();
		wij.put("name", "Battery");
		wij.put("x", 0).put("y", 6).put("w", 8).put("h", 3);
		wij.put("id", "batteryChart").put("type", "batteryChart");
		arr.put(wij);
		
		wij = new JSONObject();
		wij.put("name", "Bus/Temps");
		wij.put("x", 0).put("y", 9).put("w", 8).put("h", 2);
		wij.put("id", "busChart").put("type", "busChart");
		arr.put(wij);
		
		return NanoHTTPD.newFixedLengthResponse(root.toString());

	}

}
