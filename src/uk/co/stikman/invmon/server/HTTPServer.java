package uk.co.stikman.invmon.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.ConsoleResponse;
import uk.co.stikman.invmon.EmbeddedServer;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.HTTPServicer;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.InvMonHTTPRequest;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.client.res.ClientRes;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.nanohttpd.NanoHTTPD;
import uk.co.stikman.invmon.nanohttpd.NanoHTTPD.Response.Status;
import uk.co.stikman.invmon.server.widgets.PageWidget;
import uk.co.stikman.invmon.tooling.DevMode;
import uk.co.stikman.log.StikLog;

public class HTTPServer extends InvModule {

	private static final int GRID_SIZE = 40;

	private interface FetchMethod {
		InvMonHTTPResponse fetch(String url, UserSesh sesh, InvMonHTTPRequest http) throws Exception;
	}

	public class HandlerMapping {
		private final Pattern		pattern;
		private final FetchMethod	method;
		private UserRole			requiredRole;

		public HandlerMapping(String match, UserRole role, FetchMethod method) {
			super();
			this.requiredRole = role;
			this.pattern = Pattern.compile("^" + match + "$");
			this.method = method;
		}

		public boolean test(String resource) {
			return pattern.matcher(resource).matches();
		}

		public UserRole getRequiredRole() {
			return requiredRole;
		}

		public FetchMethod getMethod() {
			return method;
		}

	}

	private static final StikLog		LOGGER			= StikLog.getLogger(HTTPServer.class);

	private int							port;
	private final List<HandlerMapping>	mappings		= new ArrayList<>();
	private Map<String, UserSesh>		sessions		= new HashMap<>();
	private HttpLayoutConfig			layoutConfig;
	private Users						users			= new Users();
	private AuthedSessions				authedSessions	= new AuthedSessions();
	private int							requestCounter	= 0;

	private EmbeddedServer				embeddedSvr;

	private DevMode						devmode			= null;

	private SecurityMode				securityMode;

	public HTTPServer(String id, Env env) {
		super(id, env);
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

	/**
	 * "execute" returns a JSONObject specific to the widget. if everything is ok
	 * then the status response is 400, otherwise you will get 500 and a JSONObject
	 * that looks like <code>{"error":"Message goes here"}</code>
	 * 
	 * @param url
	 * @param sesh
	 * @param session
	 * @return
	 * @throws Exception
	 */
	private InvMonHTTPResponse executeApi(String url, UserSesh sesh, InvMonHTTPRequest session) throws Exception {
		String api = null;
		String id = null;
		try {
			JSONObject jo = getRequestJSONObject(session);
			JSONObject args = new JSONObject(jo.optString("args", "{}"));
			id = jo.getString("id");
			api = jo.getString("api");

			if (id == null)
				throw new NotFoundException("No Widget ID");
			if (api == null)
				throw new NotFoundException("No API Specified");

			ViewOptions viewopts = PageWidget.getViewOpts(sesh);
			PageLayout layout = viewopts.getLayout();
			if (layout == null)
				layout = layoutConfig.getDefaultPage();

			for (PageWidget wij : layout.getWidgets()) {
				if (wij.getId().equals(id)) {
					JSONObject result = wij.executeApi(sesh, api, args);
					if (result == null)
						return new InvMonHTTPResponse("{}");
					else
						return new InvMonHTTPResponse(result.toString());
				}
			}
			throw new NoSuchElementException("Widget [" + id + "] not found");

		} catch (Exception e) {
			JSONObject res = new JSONObject();
			LOGGER.error("Exception while executing api [" + api + "] on [" + id + "]");
			LOGGER.error(e);
			if (e instanceof InvMonClientError) {
				res.put("error", e.getMessage());
			} else {
				res.put("error", "Internal Error");
			}
			return new InvMonHTTPResponse(Status.INTERNAL_ERROR, "text/plain", res.toString());
		}
	}

	/**
	 * get the body (if POST) or query string (if GET) and decode it as json
	 * 
	 * @param session
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private JSONObject getRequestJSONObject(InvMonHTTPRequest session) throws InvMonException {
		try {
			String data;
			if (session.isMethod("POST"))
				data = session.getBodyAsString();
			else
				data = URLDecoder.decode(session.getQueryParameterString(), StandardCharsets.UTF_8.name());
			JSONObject jo = data == null ? new JSONObject() : new JSONObject(data);
			return jo;
		} catch (Exception e) {
			throw new InvMonException("Could not decode request data: " + e.getMessage(), e);
		}
	}

	private InvMonHTTPResponse setParams(String url, UserSesh sesh, InvMonHTTPRequest session) throws Exception {
		if (!session.getMethod().equals("POST"))
			throw new Exception("Must be POST");
		String s = session.getBodyAsString();
		JSONObject args = new JSONObject(s);
		int dur = args.getInt("dur");
		int off = args.getInt("off");
		ViewOptions global = PageWidget.getViewOpts(sesh);
		global.setDuration(dur);
		global.setOffset(off);
		//		global.setLayout(this.layoutConfig.getPage(jo.optString("page", null)));
		sesh.putData(WebUtils.CACHED_QUERY_RESULTS, null);
		sesh.putData(WebUtils.CACHED_LAST_RECORD, null);
		return new InvMonHTTPResponse("{}");
	}

	private InvMonHTTPResponse toStringHandler(String url, UserSesh sesh, InvMonHTTPRequest session) throws Exception {
		return new InvMonHTTPResponse("TODO: not implemented");
	}

	private InvMonHTTPResponse logout(String url, UserSesh sesh, InvMonHTTPRequest session) throws Exception {
		sesh.setAuthedUserSession(null);
		return new InvMonHTTPResponse("{}");
	}

	private InvMonHTTPResponse login(String url, UserSesh sesh, InvMonHTTPRequest session) throws Exception {
		if (!session.getMethod().equals("POST"))
			throw new Exception("Must be POST");
		String s = session.getBodyAsString();
		JSONObject jo = new JSONObject(s);
		try {
			User u = users.getByName(jo.getString("user"));
			if (!u.getPass().equals(jo.getString("pass")))
				throw new InvMonException("Incorrect password");

			AuthedSession as = authedSessions.createNew(u);
			sesh.setAuthedUserSession(as);
			JSONObject res = new JSONObject();
			res.put("name", jo.getString("user"));
			res.put("token", as.getUid());
			return new InvMonHTTPResponse(res.toString());

		} catch (Exception e) {
			LOGGER.error("Login for user [" + jo.getString("user") + "] failed because: " + e.getMessage(), e);
			throw new InvMonClientError("Login failed"); // don't pass any details back here 
		}

	}

	@Override
	public void configure(Element config) throws InvMonException {
		this.port = Integer.parseInt(InvUtil.getAttrib(config, "port"));
		Element el = InvUtil.getElement(config, "DevMode", true);
		if (el != null) {
			devmode = new DevMode(el);
		}
		layoutConfig = new HttpLayoutConfig();
		layoutConfig.configure(getEnv(), InvUtil.miniDomFromReal(config));
		users.configure(config);
		String s = InvUtil.getAttrib(config, "security", "simple-user");
		securityMode = SecurityMode.valueOf(s.toUpperCase());
	}

	public SecurityMode getSecurityMode() {
		return securityMode;
	}

	@Override
	public void start() throws InvMonException {
		super.start();

		if (devmode != null)
			mappings.add(new HandlerMapping("classes\\.js", UserRole.PUBLIC, devmode::serve));
		mappings.add(new HandlerMapping(".*\\.(gif|png|css|html|js|svg)", UserRole.PUBLIC, this::resource));

		mappings.add(new HandlerMapping("log", UserRole.PUBLIC, this::logPage));
		mappings.add(new HandlerMapping("data", UserRole.PUBLIC, this::dataPage));

		mappings.add(new HandlerMapping("api", UserRole.PUBLIC, this::executeApi));
		mappings.add(new HandlerMapping("toString", UserRole.PUBLIC, this::toStringHandler));
		mappings.add(new HandlerMapping("setParams", UserRole.PUBLIC, this::setParams));
		mappings.add(new HandlerMapping("fetchUserLog", UserRole.PUBLIC, this::fetchUserLog));
		mappings.add(new HandlerMapping("getUserDetails", UserRole.PUBLIC, this::getUserDetails));

		mappings.add(new HandlerMapping("console", UserRole.PUBLIC, this::execConsoleCommand));

		mappings.add(new HandlerMapping("getConfig", UserRole.PUBLIC, this::getConfig));
		mappings.add(new HandlerMapping("saveConfig", UserRole.ADMIN, this::savePageConfig));
		mappings.add(new HandlerMapping("invalidateResults", UserRole.PUBLIC, this::invalidateResults));
		mappings.add(new HandlerMapping("login", UserRole.PUBLIC, this::login));
		mappings.add(new HandlerMapping("logout", UserRole.PUBLIC, this::logout));

		getEnv().submitTimerTask(() -> getEnv().submitTask(this::tidySessions), 60000);

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
			++requestCounter;
			if (!http.isMethod("GET") && !http.isMethod("POST"))
				throw new Exception("Unsupported method");

			String url = http.getUri();
			if (url.equals("/"))
				url = "/index.html";
			url = url.substring(1);

			HandlerMapping m = null;
			for (HandlerMapping x : mappings) {
				if (x.test(url)) {
					m = x;
					break;
				}
			}

			if (m == null)
				throw new NotFoundException("Not found");

			String seshId = http.getCookies().get("sesh");
			boolean setSeshCookie = false;
			UserSesh sesh;
			synchronized (sessions) {
				sesh = seshId == null ? null : sessions.get(seshId);
				if (sesh == null) {
					sesh = new UserSesh(this);
					sessions.put(sesh.getId(), sesh);
					setSeshCookie = true;
				}
			}
			sesh.touch();


			sesh.requireUserRole(m.getRequiredRole());
			synchronized (sesh) {
				InvMonHTTPResponse res = m.getMethod().fetch(url, sesh, http);
				if (setSeshCookie)
					res.addHeader("Set-Cookie", "sesh=" + sesh.getId());
				return res;
			}

		} catch (NotFoundException nfe) {
			return new InvMonHTTPResponse(Status.NOT_FOUND, "text/html", "404 Not Found: " + nfe.getMessage());
		} catch (Exception e) {
			LOGGER.error(e);
			if (e instanceof InvMonClientError)
				return new InvMonHTTPResponse(Status.INTERNAL_ERROR, "text/plain", e.getMessage());
			else
				return new InvMonHTTPResponse(Status.INTERNAL_ERROR, "text/plain", "Internal Error");
		}
	}

	private InvMonHTTPResponse invalidateResults(String url, UserSesh sesh, InvMonHTTPRequest request) {
		sesh.putData(WebUtils.CACHED_QUERY_RESULTS, null);
		sesh.putData(WebUtils.CACHED_LAST_RECORD, null);
		return new InvMonHTTPResponse(new JSONObject().put("result", "OK").toString());
	}

	private InvMonHTTPResponse execConsoleCommand(String url, UserSesh sesh, InvMonHTTPRequest request) throws Exception {
		if (!request.getMethod().equals("POST"))
			throw new Exception("Must be POST");

		String s = request.getBodyAsString();
		LOGGER.info("Console command: " + s);
		JSONObject jo = new JSONObject(s);
		try {
			JSONObject res = new JSONObject();

			//
			// find the Console object that should be hanging around, otherwise 
			// make one
			//
			Console c;
			synchronized (sesh) {
				c = sesh.getData("console");
				if (c == null)
					sesh.putData("console", c = new Console(getEnv(), sesh));
			}

			String cmd = jo.getString("cmd");
			res.put("status", "ok");
			if (cmd == null || cmd.isBlank()) {
				res.put("result", "-no input-");
			} else {
				ConsoleResponse output = c.execute(cmd.trim());
				res.put("result", output.getText());
				res.put("formatted", output.isFormatted());
				res.put("module", output.getActiveModule() == null ? "" : output.getActiveModule().getId());
			}
			return new InvMonHTTPResponse(res.toString());

		} catch (Exception e) {
			LOGGER.error("Console command: [" + s + "] failed: " + e.getMessage(), e);
			JSONObject res = new JSONObject();
			res.put("status", "error");
			res.put("error", e.getMessage());
			return new InvMonHTTPResponse(res.toString());
		}
	}

	private InvMonHTTPResponse getUserDetails(String url, UserSesh sesh, InvMonHTTPRequest request) throws InvMonException {
		AuthedSession u = sesh.getAuthedUserSession();
		if (u == null)
			return new InvMonHTTPResponse(Status.OK, "text/json", "{}");
		return new InvMonHTTPResponse(Status.OK, "text/json", new JSONObject().put("name", u.getUser().getName()).put("token", u.getUid()).toString());
	}

	private InvMonHTTPResponse fetchUserLog(String url, UserSesh sesh, InvMonHTTPRequest request) throws InvMonException {
		JSONObject res = new JSONObject();
		List<String> lst = getEnv().copyUserLog(new ArrayList<>());
		res.put("log", lst.stream().collect(Collectors.joining("\n")));
		return new InvMonHTTPResponse(res.toString());
	}

	private InvMonHTTPResponse savePageConfig(String url, UserSesh sesh, InvMonHTTPRequest request) throws InvMonException {
		JSONObject opts = getRequestJSONObject(request);
		String name = opts.optString("page", null);
		PageLayout pg;
		if (name == null)
			pg = layoutConfig.getDefaultPage();
		else
			pg = layoutConfig.getPage(name);

		pg.saveToSource();
		return new InvMonHTTPResponse("{}");
	}

	private InvMonHTTPResponse getConfig(String url, UserSesh sesh, InvMonHTTPRequest request) throws InvMonException {
		JSONObject opts = getRequestJSONObject(request);
		String name = opts.optString("page", null);
		PageLayout pg = null;
		if (name == null)
			pg = layoutConfig.getDefaultPage();
		else
			pg = layoutConfig.getPage(name);

		int gs = GRID_SIZE;
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

	@Override
	public String toString() {
		return "HTTPServer running on port " + port + ".  Served " + requestCounter + " requests";
	}

}
