package uk.co.stikman.invmon.htmlout;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.FieldNameList;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.client.res.ClientRes;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.datalog.MiniDbException;
import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.log.StikLog;

public class HTTPServer extends InvModule {

	private interface FetchMethod {
		Response fetch(String url, UserSesh sesh, IHTTPSession session) throws Exception;
	}

	private static final StikLog			LOGGER					= StikLog.getLogger(HTTPServer.class);

	/**
	 * keys for associating objects with a user session. in particular the cached
	 * ones are used to avoid re-querying the database for multiple hits with a
	 * single page
	 */
	private static final String				GLOBAL_VIEW_OPTIONS		= "gvo";
	private static final String				CACHED_QUERY_RESULTS	= "cqr";
	private static final String				CACHED_LAST_RECORD		= "clr";

	private DataLogger						datalogger;
	private int								port;
	private Svr								svr;
	private final Map<String, FetchMethod>	urlMappings				= new HashMap<>();
	private Map<String, UserSesh>			sessions				= new HashMap<>();
	private HttpLayoutConfig				layoutConfig;

	public HTTPServer(String id, Env env) {
		super(id, env);
		urlMappings.put("loading.gif", this::resource);
		urlMappings.put("background.png", this::resource);
		urlMappings.put("ticked.png", this::resource);
		urlMappings.put("unticked.png", this::resource);
		urlMappings.put("style.css", this::resource);
		urlMappings.put("index.html", this::resource);
		urlMappings.put("classes.js", this::resource);

		urlMappings.put("executeChart", this::executeChart);
		urlMappings.put("getConfig", this::getConfig);
		urlMappings.put("setParams", this::setParams);
		urlMappings.put("getInfoBit", this::getInfoBit);
		urlMappings.put("invalidateResults", this::invalidateResults);

		env.submitTimerTask(() -> env.submitTask(this::tidySessions), 60000);
	}

	private void tidySessions() {
		synchronized (sessions) {
			Set<String> remove = new HashSet<>();
			for (Entry<String, UserSesh> e : sessions.entrySet()) {
				if (e.getValue().hasExpired())
					remove.add(e.getKey());
			}
			remove.forEach(sessions::remove);
		}
	}

	/**
	 * just a file without anything else
	 * 
	 * @param session
	 * @return
	 */
	private Response resource(String url, UserSesh sesh, IHTTPSession session) throws Exception {
		ClientRes r = ClientRes.get(url);
		return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.getMimeTypeForFile(url), r.makeStream(), r.getSize());
	}

	private Response executeChart(String url, UserSesh sesh, IHTTPSession session) throws Exception {
		JSONObject jo = new JSONObject(URLDecoder.decode(session.getQueryParameterString(), StandardCharsets.UTF_8.name()));
		String name = jo.getString("name");
		if (name == null)
			throw new NotFoundException("No chart name");

		ViewOptions viewopts = sesh.getData(GLOBAL_VIEW_OPTIONS);
		if (viewopts == null) {
			viewopts = new ViewOptions();
			sesh.putData(GLOBAL_VIEW_OPTIONS, viewopts);
		}
		ChartRenderConfig opts = new ChartRenderConfig();
		opts.setDuration(viewopts.getDuration());
		opts.setOffset(viewopts.getOffset());
		opts.setWidth(jo.optInt("w", 700));
		opts.setHeight(jo.optInt("h", 300));

		ensureCachedResults(sesh);
		QueryResults qr = sesh.getData(CACHED_QUERY_RESULTS);
		DBRecord lastrec = sesh.getData(CACHED_LAST_RECORD);

		PageLayout layout = viewopts.getLayout();
		if (layout == null)
			layout = layoutConfig.getDefaultPage();

		WidgetExecuteContext ctx = new WidgetExecuteContext(this, sesh, qr, lastrec);
		for (PageWidget wij : layout.getWidgets()) {
			if (wij.getId().equals(name)) {
				JSONObject result = wij.execute(jo, ctx);
				return NanoHTTPD.newFixedLengthResponse(Status.OK, "text/html", result.toString());
			}
		}

		JSONObject res = new JSONObject();
		HTMLBuilder html = new HTMLBuilder();
		html.append("Widget [" + name + "] not found");
		res.put("contentHtml", html.toString());
		return NanoHTTPD.newFixedLengthResponse(Status.OK, "text/html", res.toString());
	}

	private void ensureCachedResults(UserSesh sesh) {
		synchronized (sesh) {
			ViewOptions opts = sesh.getData(GLOBAL_VIEW_OPTIONS);
			QueryResults qr = sesh.getData(CACHED_QUERY_RESULTS);
			if (qr == null) {
				long end = System.currentTimeMillis();
				long start = end - (long) opts.getDuration() * 1000 * 60;
				FieldNameList flds = new FieldNameList();
				//
				// add everything except timestamp, i guess
				//
				for (Field f : getEnv().getModel())
					if (!f.getId().equals("TIMESTAMP"))
						flds.add(f.getId());

				try {
					QueryResults aggr = datalogger.query(start, end, 100, flds.asList());
					DBRecord lr = datalogger.getLastRecord();
					sesh.putData(CACHED_QUERY_RESULTS, aggr);
					sesh.putData(CACHED_LAST_RECORD, datalogger.getLastRecord());
				} catch (MiniDbException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public void configure(Element config) {
		this.port = Integer.parseInt(InvUtil.getAttrib(config, "port"));
		layoutConfig = new HttpLayoutConfig();
		layoutConfig.configure(config);
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
		if (svr != null)
			svr.stop();
		super.terminate();
	}

	@Subscribe(Events.POST_DATA)
	public void postData(PollData data) {
	}

	private Response serve(IHTTPSession httpsession) {
		try {
			if (httpsession.getMethod() != Method.GET && httpsession.getMethod() != Method.POST)
				throw new Exception("Unsupported method");

			String url = httpsession.getUri();
			if (url.equals("/"))
				url = "/index.html";
			url = url.substring(1);

			FetchMethod m = urlMappings.get(url);
			if (m == null)
				throw new NotFoundException("Not found");

			String seshId = httpsession.getCookies().read("sesh");
			boolean setSeshCookie = false;
			UserSesh sesh;
			synchronized (sessions) {
				sesh = sessions.get(seshId);
				if (sesh == null) {
					sesh = new UserSesh();
					sessions.put(sesh.getId(), sesh);
					setSeshCookie = true;
				}
			}
			sesh.touch();

			Response res = m.fetch(url, sesh, httpsession);
			if (setSeshCookie)
				res.addHeader("Set-Cookie", "sesh=" + sesh.getId());
			return res;

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

	private Response getInfoBit(String url, UserSesh sesh, IHTTPSession request) throws Exception {
		JSONObject jo = new JSONObject(URLDecoder.decode(request.getQueryParameterString(), StandardCharsets.UTF_8.name()));
		String name = jo.getString("name");
		if (name == null)
			throw new NotFoundException("No widget name");

		ViewOptions viewopts = sesh.getData(GLOBAL_VIEW_OPTIONS);
		if (viewopts == null)
			sesh.putData(GLOBAL_VIEW_OPTIONS, viewopts = new ViewOptions());
		PageLayout layout = viewopts.getLayout();
		if (layout == null)
			layout = layoutConfig.getDefaultPage();
		WidgetExecuteContext ctx = new WidgetExecuteContext(this, sesh, null, null);
		return NanoHTTPD.newFixedLengthResponse(Status.OK, "text/html", layout.getWidgetById(name).execute(null, ctx).toString());
	}

	private Response setParams(String url, UserSesh sesh, IHTTPSession request) {
		JSONObject jo = decodeQueryParams(request);
		int dur = jo.getInt("dur");
		int off = jo.getInt("off");
		ViewOptions global = sesh.getData(GLOBAL_VIEW_OPTIONS);
		if (global == null)
			sesh.putData(GLOBAL_VIEW_OPTIONS, global = new ViewOptions());
		global.setDuration(dur);
		global.setOffset(off);
		global.setLayout(this.layoutConfig.getPage(jo.optString("page", null)));
		sesh.putData(CACHED_QUERY_RESULTS, null);
		sesh.putData(CACHED_LAST_RECORD, null);
		return NanoHTTPD.newFixedLengthResponse(new JSONObject().put("result", "OK").toString());
	}

	private static JSONObject decodeQueryParams(IHTTPSession request) {
		try {
			JSONObject jo = new JSONObject(URLDecoder.decode(request.getQueryParameterString(), StandardCharsets.UTF_8.name()));
			return jo;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("URLDecoded failed: " + e.getMessage(), e);
		}
	}

	private Response invalidateResults(String url, UserSesh sesh, IHTTPSession request) {
		sesh.putData(CACHED_QUERY_RESULTS, null);
		sesh.putData(CACHED_LAST_RECORD, null);
		return NanoHTTPD.newFixedLengthResponse(new JSONObject().put("result", "OK").toString());
	}

	private Response getConfig(String url, UserSesh sesh, IHTTPSession request) {
		JSONObject opts = decodeQueryParams(request);
		String name = opts.optString("page", null);
		PageLayout pg = null;
		if (name == null)
			pg = layoutConfig.getDefaultPage();
		else
			pg = layoutConfig.getPage(name);

		int gs = 40;
		JSONObject root = new JSONObject();
		root.put("gridSize", gs);
		JSONArray arr = new JSONArray();
		root.put("widgets", arr);

		for (PageWidget w : pg.getWidgets()) {
			JSONObject wij = new JSONObject();
			wij.put("x", w.getX()).put("y", w.getY()).put("w", w.getWidth()).put("h", w.getHeight());
			wij.put("id", w.getId()).put("type", w.getClientWidgetType());
			wij.put("title", w.getTitle());
			arr.put(wij);
		}

		JSONObject wij = new JSONObject();
		wij.put("x", 0).put("y", 0).put("w", 25).put("h", 1);
		wij.put("id", "timeselector").put("type", "timesel");
		arr.put(wij);

		for (int i = 0; i < arr.length(); ++i) {
			JSONObject jo = arr.getJSONObject(i);
			jo.put("x", jo.getInt("x") * gs);
			jo.put("y", jo.getInt("y") * gs);
			jo.put("w", jo.getInt("w") * gs);
			jo.put("h", jo.getInt("h") * gs);
		}

		HTMLBuilder html = new HTMLBuilder();
		html.append("<div class=\"tiny\"><div class=\"a\">Local Time: </div><div class=\"b\">").append(new Date().toString()).append("</div></div>");
		html.append("<div class=\"tiny\"><div class=\"a\">Version: </div><div class=\"b\">").append(Env.getVersion()).append("</div></div>");
		root.put("infoBit", html.toString());

		return NanoHTTPD.newFixedLengthResponse(root.toString());

	}

	@Subscribe(Events.LOGGER_RECORD_COMMITED)
	public void onDBChanged(DBRecord rec) {
		synchronized (sessions) {

		}
	}

	public DataLogger getTargetModule() {
		return datalogger;
	}

}
