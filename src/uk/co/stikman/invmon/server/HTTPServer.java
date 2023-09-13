package uk.co.stikman.invmon.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.EmbeddedServer;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.FieldNameList;
import uk.co.stikman.invmon.HTTPServicer;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.InvMonHTTPRequest;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.client.res.ClientRes;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.datalog.MiniDbException;
import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.nanohttpd.NanoHTTPD;
import uk.co.stikman.invmon.nanohttpd.NanoHTTPD.Response.Status;
import uk.co.stikman.invmon.server.widgets.PageWidget;
import uk.co.stikman.log.StikLog;

public class HTTPServer extends InvModule {

	private interface FetchMethod {
		InvMonHTTPResponse fetch(String url, UserSesh sesh, InvMonHTTPRequest http) throws Exception;
	}

	private static final StikLog			LOGGER					= StikLog.getLogger(HTTPServer.class);

	/**
	 * keys for associating objects with a user session. in particular the
	 * cached ones are used to avoid re-querying the database for multiple hits
	 * with a single page
	 */
	public static final String				GLOBAL_VIEW_OPTIONS		= "gvo";
	public static final String				CACHED_QUERY_RESULTS	= "cqr";
	public static final String				CACHED_LAST_RECORD		= "clr";

	private DataLogger						datalogger;
	private int								port;
	private final Map<String, FetchMethod>	urlMappings				= new HashMap<>();
	private Map<String, UserSesh>			sessions				= new HashMap<>();
	private HttpLayoutConfig				layoutConfig;

	private EmbeddedServer					embeddedSvr;

	public HTTPServer(String id, Env env) {
		super(id, env);
		urlMappings.put("loading.gif", this::resource);
		urlMappings.put("background.png", this::resource);
		urlMappings.put("ticked.png", this::resource);
		urlMappings.put("unticked.png", this::resource);
		urlMappings.put("style.css", this::resource);
		urlMappings.put("index.html", this::resource);
		urlMappings.put("classes.js", this::resource);

		urlMappings.put("log", this::logPage);
		urlMappings.put("data", this::dataPage);

		urlMappings.put("executeChart", this::executeChart);
		urlMappings.put("getConfig", this::getConfig);
		urlMappings.put("setParams", this::setParams);
		urlMappings.put("getInfoBit", this::getInfoBit);
		urlMappings.put("invalidateResults", this::invalidateResults);

		urlMappings.put("login", this::login);

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
	private InvMonHTTPResponse resource(String url, UserSesh sesh, InvMonHTTPRequest session) throws Exception {
		ClientRes r = ClientRes.get(url);
		return new InvMonHTTPResponse(Status.OK, NanoHTTPD.getMimeTypeForFile(url), r.makeStream(), r.getSize());
	}

	private InvMonHTTPResponse dataPage(String url, UserSesh sesh, InvMonHTTPRequest session) throws Exception {
		return new DataViewPage(this).exec(url, sesh, session);
	}

	private InvMonHTTPResponse logPage(String url, UserSesh sesh, InvMonHTTPRequest session) throws Exception {
		String html = new LogPage(getEnv(), sesh).exec();
		return new InvMonHTTPResponse(html);
	}

	private InvMonHTTPResponse executeChart(String url, UserSesh sesh, InvMonHTTPRequest session) throws Exception {
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
				return new InvMonHTTPResponse(result.toString());
			}
		}

		JSONObject res = new JSONObject();
		HTMLBuilder html = new HTMLBuilder();
		html.append("Widget [" + name + "] not found");
		res.put("contentHtml", html.toString());
		return new InvMonHTTPResponse(res.toString());
	}

	private InvMonHTTPResponse login(String url, UserSesh sesh, InvMonHTTPRequest session) throws Exception {
		if (!session.getMethod().equals("POST")) 
			throw new Exception("Must be POST");
		String s = session.getBodyAsString();
		JSONObject jo = new JSONObject(s);
		
		
		JSONObject res = new JSONObject();
		res.put("name", jo.getString("user"));
		res.put("token", "askdja;slkdaj");
		return new InvMonHTTPResponse(res.toString());
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
					sesh.putData(CACHED_QUERY_RESULTS, aggr);
					sesh.putData(CACHED_LAST_RECORD, datalogger.getLastRecord());
				} catch (MiniDbException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public void configure(Element config) throws InvMonException {
		this.port = Integer.parseInt(InvUtil.getAttrib(config, "port"));
		layoutConfig = new HttpLayoutConfig();
		layoutConfig.configure(config);
	}

	@Override
	public void start() throws InvMonException {
		super.start();
		this.datalogger = getEnv().getModule("datalogger");

		HTTPServicer intf = new HTTPServicer() {
			@Override
			public InvMonHTTPResponse serve(InvMonHTTPRequest http) {
				InvMonHTTPResponse res = HTTPServer.this.serve(http);
				return res;
			}
		};

		embeddedSvr = new EmbeddedServer(port, intf);
		try {
			embeddedSvr.start();
		} catch (IOException e) {
			throw new InvMonException("Could not start NanoHTTPD: " + e.getMessage(), e);
		}
	}

	@Override
	public void terminate() {
		if (embeddedSvr != null)
			embeddedSvr.stop();
		super.terminate();
	}

	@Subscribe(Events.POST_DATA)
	public void postData(PollData data) {
	}

	private InvMonHTTPResponse serve(InvMonHTTPRequest http) {
		try {
			if (!http.isMethod("GET") && !http.isMethod("POST"))
				throw new Exception("Unsupported method");

			String url = http.getUri();
			if (url.equals("/"))
				url = "/index.html";
			url = url.substring(1);

			FetchMethod m = urlMappings.get(url);
			if (m == null)
				throw new NotFoundException("Not found");

			String seshId = http.getCookies().get("sesh");
			boolean setSeshCookie = false;
			UserSesh sesh;
			synchronized (sessions) {
				sesh = seshId == null ? null : sessions.get(seshId);
				if (sesh == null) {
					sesh = new UserSesh();
					sessions.put(sesh.getId(), sesh);
					setSeshCookie = true;
				}
			}
			sesh.touch();

			InvMonHTTPResponse res = m.fetch(url, sesh, http);
			if (setSeshCookie)
				res.addHeader("Set-Cookie", "sesh=" + sesh.getId());
			return res;

		} catch (NotFoundException nfe) {
			return new InvMonHTTPResponse(Status.NOT_FOUND, "text/html", "404 Not Found: " + nfe.getMessage());
		} catch (Exception e) {
			LOGGER.error(e);
			return new InvMonHTTPResponse(Status.INTERNAL_ERROR, "text/html", "Internal Error");
		}
	}

	private InvMonHTTPResponse getInfoBit(String url, UserSesh sesh, InvMonHTTPRequest request) throws Exception {
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
		return new InvMonHTTPResponse(Status.OK, "text/html", layout.getWidgetById(name).execute(null, ctx).toString());
	}

	private InvMonHTTPResponse setParams(String url, UserSesh sesh, InvMonHTTPRequest request) throws InvMonException {
		JSONObject jo = new JSONObject(request.getBodyAsString());
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
		return new InvMonHTTPResponse(new JSONObject().put("result", "OK").toString());
	}

	private static JSONObject decodeQueryParams(InvMonHTTPRequest request) {
		try {
			JSONObject jo = new JSONObject(URLDecoder.decode(request.getQueryParameterString(), StandardCharsets.UTF_8.name()));
			return jo;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("URLDecoded failed: " + e.getMessage(), e);
		}
	}

	private InvMonHTTPResponse invalidateResults(String url, UserSesh sesh, InvMonHTTPRequest request) {
		sesh.putData(CACHED_QUERY_RESULTS, null);
		sesh.putData(CACHED_LAST_RECORD, null);
		return new InvMonHTTPResponse(new JSONObject().put("result", "OK").toString());
	}

	private InvMonHTTPResponse getConfig(String url, UserSesh sesh, InvMonHTTPRequest request) throws InvMonException {
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

		return new InvMonHTTPResponse(Status.OK, "text/html", root.toString());

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
