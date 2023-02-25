package uk.co.stikman.invmon.htmlout;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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
import uk.co.stikman.invmon.FieldNameList;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.datalog.MiniDbException;
import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.datamodel.VIFReading;
import uk.co.stikman.invmon.htmlout.res.Res;
import uk.co.stikman.invmon.inverter.InvUtil;
import uk.co.stikman.log.StikLog;

public class HTTPServer extends InvModule {

	private interface FetchMethod {
		Response fetch(String url, UserSesh sesh, IHTTPSession session) throws Exception;
	}

	private static final StikLog			LOGGER				= StikLog.getLogger(HTTPServer.class);
	private static final String				GLOBAL_VIEW_OPTIONS	= "gvo";
	private static final String				LAST_QUERY_RESULTS	= "lqr";
	private DataLogger						datalogger;
	private int								port;
	private Svr								svr;
	private PollData						lastData;
	private final Map<String, FetchMethod>	urlMappings			= new HashMap<>();
	private HTMLGenerator					generator;
	private Map<String, UserSesh>			sessions			= new HashMap<>();

	public HTTPServer(String id, Env env) {
		super(id, env);
		urlMappings.put("old.html", this::staticPage);
		urlMappings.put("loading.gif", this::page);
		urlMappings.put("background.png", this::page);
		urlMappings.put("style.css", this::page);
		urlMappings.put("index.html", this::page);
		urlMappings.put("main.js", this::page);
		urlMappings.put("util.js", this::page);

		urlMappings.put("getSectData", this::fetchSectionData);
		urlMappings.put("getConfig", this::getPageConfig);
		urlMappings.put("setParams", this::setParams);

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
	private Response page(String url, UserSesh sesh, IHTTPSession session) throws Exception {
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
	private Response staticPage(String url, UserSesh sesh, IHTTPSession session) throws Exception {
		String offset = getParam(session, "off");
		String duration = getParam(session, "dur");

		ChartRenderConfig opts = new ChartRenderConfig();
		opts.setDuration(duration == null ? 60 * 10 : Long.parseLong(duration));
		opts.setOffset(offset == null ? 0 : Long.parseLong(offset));

		HTMLBuilder html = new HTMLBuilder();
		new HTMLGenerator(datalogger).render(html, opts);
		return NanoHTTPD.newFixedLengthResponse(Status.OK, "text/html", html.toString());
	}

	private Response fetchSectionData(String url, UserSesh sesh, IHTTPSession session) throws Exception {

		JSONObject jo = new JSONObject(URLDecoder.decode(session.getQueryParameterString(), StandardCharsets.UTF_8));
		String name = jo.getString("name");
		if (name == null)
			throw new NotFoundException("No chart name");

		ViewOptions viewopts = sesh.getData(GLOBAL_VIEW_OPTIONS);
		ChartRenderConfig opts = new ChartRenderConfig();
		opts.setDuration(viewopts.getDuration());
		opts.setOffset(viewopts.getOffset());
		opts.setWidth(jo.optInt("w", 700));
		opts.setHeight(jo.optInt("h", 300));

		JSONObject res = new JSONObject();
		HTMLBuilder html = new HTMLBuilder();
		List<String> titleBits = new ArrayList<>();
		QueryResults data = getQueryResults(sesh);
		if (name.equals("pvChart")) {
			generator.renderPVPowerChart(html, opts, data);
			titleBits.add(generator.renderGrp(new HTMLBuilder(), "<div class=\"grp\">Total: [%d]W</div>", (int) data.getLastRecord().getFloat("PV_TOTAL_P")).toString());
		} else if (name.equals("loadChart")) {
			generator.renderLoadChart(html, opts, data);
			VIFReading vif1 = data.getLastRecord().getVIF("LOAD");
			titleBits.add(generator.renderGrp(new HTMLBuilder(), "<div class=\"grp\">Load: [%d]W ([%.2f]V @ [%.2f]A)</div>", (int) vif1.getP(), vif1.getV(), vif1.getI()).toString());
			float pf = data.getLastRecord().getFloat("LOAD_PF");
			titleBits.add(generator.renderGrp(new HTMLBuilder(), "<div class=\"grp\">PF: [%.2f] (Real Power: [%d]W @ [%.2f]A)</div>", pf, (int) (vif1.getP() * pf), vif1.getI() * pf).toString());
		} else if (name.equals("batteryChart")) {
			generator.renderBatteryChart(html, opts, data);
			VIFReading vif1 = data.getLastRecord().getVIF("BATT");
			titleBits.add(generator.renderVIF(new HTMLBuilder(), "Batt", vif1).toString());
		} else if (name.equals("busChart")) {
			generator.renderTempChart(html, opts, data);
			float ftmp1 = data.getLastRecord().getFloat("INV_1_TEMP");
			float ftmp2 = data.getLastRecord().getFloat("INV_2_TEMP");
			float busv1 = data.getLastRecord().getFloat("INV_1_BUS_V");
			float busv2 = data.getLastRecord().getFloat("INV_2_BUS_V");

			titleBits.add(generator.renderGrp(new HTMLBuilder(), "<div class=\"grp\">Temp1: [%.1f]C  BusV: [%d]V</div>", ftmp1, (int) busv1).toString());
			titleBits.add(generator.renderGrp(new HTMLBuilder(), "<div class=\"grp\">Temp2: [%.1f]C  BusV: [%d]V</div>", ftmp2, (int) busv2).toString());
		} else if (name.equals("pvTable")) {
			generator.renderPVTable(html, opts, data);
		} else
			html.append("NOT FOUND");

		JSONArray arr = new JSONArray();
		titleBits.forEach(arr::put);
		res.put("titleBits", arr);
		res.put("contentHtml", html.toString());
		return NanoHTTPD.newFixedLengthResponse(Status.OK, "text/html", res.toString());
	}

	private QueryResults getQueryResults(UserSesh sesh) {
		synchronized (sesh) {
			ViewOptions opts = sesh.getData(GLOBAL_VIEW_OPTIONS);
			QueryResults qr = sesh.getData(LAST_QUERY_RESULTS);
			if (qr == null) {
				long end = System.currentTimeMillis();
				long start = end - (long) opts.getDuration() * 1000 * 60;
				FieldNameList flds = new FieldNameList();
				flds.add("BATT_V, BATT_I, BATT_I_CHG, BATT_I_DIS");
				flds.add("LOAD_V, LOAD_I, LOAD_F");
				flds.add("LOAD_P, LOAD_1_P, LOAD_2_P");
				flds.add("PVA_1_V, PVA_1_I, PVA_1_P");
				flds.add("PVB_1_V, PVB_1_I, PVB_1_P");
				flds.add("PVA_2_V, PVA_2_I, PVA_2_P");
				flds.add("PVB_2_V, PVB_2_I, PVB_2_P");
				flds.add("PV_TOTAL_P");
				flds.add("INV_1_TEMP,INV_2_TEMP");
				flds.add("INV_1_BUS_V,INV_2_BUS_V");
				flds.add("LOAD_PF");
				try {
					qr = datalogger.query(start, end, 100, flds.asList());
				} catch (MiniDbException e) {
					throw new RuntimeException(e);
				}
				sesh.putData(LAST_QUERY_RESULTS, qr);
			}
			return qr;
		}
	}

	@Override
	public void configure(Element config) {
		this.port = Integer.parseInt(InvUtil.getAttrib(config, "port"));
	}

	@Override
	public void start() throws InvMonException {
		super.start();
		this.datalogger = getEnv().getModule("datalogger");
		generator = new HTMLGenerator(datalogger);
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
		this.lastData = data;
	}

	private Response serve(IHTTPSession httpsession) {
		try {
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

	private Response setParams(String url, UserSesh sesh, IHTTPSession request) {
		JSONObject jo = new JSONObject(URLDecoder.decode(request.getQueryParameterString(), StandardCharsets.UTF_8));
		int dur = jo.getInt("dur");
		int off = jo.getInt("off");
		ViewOptions global = sesh.getData(GLOBAL_VIEW_OPTIONS);
		if (global == null)
			sesh.putData(GLOBAL_VIEW_OPTIONS, global = new ViewOptions());
		global.setDuration(dur);
		global.setOffset(off);
		sesh.putData(LAST_QUERY_RESULTS, null);
		return NanoHTTPD.newFixedLengthResponse("OK");
	}

	private Response getPageConfig(String url, UserSesh sesh, IHTTPSession request) {
		String name = InvUtil.getParam(request, "layout");
		if (name == null)
			name = "default";

		// TODO: load layouts

		JSONObject root = new JSONObject();
		root.put("gridSize", 40);
		JSONArray arr = new JSONArray();
		root.put("widgets", arr);

		JSONObject wij = new JSONObject();
		wij.put("x", 0).put("y", 0).put("w", 25).put("h", 1);
		wij.put("id", "timeselector").put("type", "timesel");
		arr.put(wij);

		wij = new JSONObject();
		wij.put("name", "PV Power");
		wij.put("x", 0).put("y", 1).put("w", 20).put("h", 7);
		wij.put("id", "pvChart").put("type", "pvChart");
		arr.put(wij);

		wij = new JSONObject();
		wij.put("name", "PV Power");
		wij.put("x", 20).put("y", 1).put("w", 5).put("h", 5);
		wij.put("id", "pvTable").put("type", "pvTable");
		arr.put(wij);

		wij = new JSONObject();
		wij.put("name", "Load");
		wij.put("x", 0).put("y", 8).put("w", 20).put("h", 7);
		wij.put("id", "loadChart").put("type", "loadChart");
		arr.put(wij);

		wij = new JSONObject();
		wij.put("name", "Battery");
		wij.put("x", 0).put("y", 15).put("w", 20).put("h", 7);
		wij.put("id", "batteryChart").put("type", "batteryChart");
		arr.put(wij);

		wij = new JSONObject();
		wij.put("name", "Bus/Temps");
		wij.put("x", 0).put("y", 22).put("w", 20).put("h", 4);
		wij.put("id", "busChart").put("type", "busChart");
		arr.put(wij);
		
		wij = new JSONObject();
		wij.put("name", "infobit");
		wij.put("x", 0).put("y", 26).put("w", 20).put("h", 2);
		wij.put("id", "infobit").put("type", "infobit");
		arr.put(wij);

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

}
