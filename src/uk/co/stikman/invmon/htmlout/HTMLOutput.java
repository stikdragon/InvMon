package uk.co.stikman.invmon.htmlout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.HTMLConsoleThing;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.TempHTMLOutput;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.inverter.InvUtil;
import uk.co.stikman.log.StikLog;

public class HTMLOutput extends InvModule {
	private static final StikLog	LOGGER	= StikLog.getLogger(HTMLOutput.class);
	private File					target;
	private long					lastT;
	private DataLogger				datalogger;

	public HTMLOutput(String id, Env env) {
		super(id, env);
	}

	@Override
	public void configure(Element config) {
		this.target = new File(InvUtil.getAttrib(config, "target"));
	}

	@Override
	public void start() throws InvMonException {
		super.start();
		this.datalogger = getEnv().getModule("datalogger");
	}

	@Subscribe(Events.POST_DATA)
	public void postData(PollData data) {
		long dt = System.currentTimeMillis() - lastT;
		if (dt > 2000) {
			lastT = System.currentTimeMillis();

			//
			// render a nice page
			//
			HTMLBuilder html = new HTMLBuilder();
			html.append(getClass(), "top.html");

			html.div("sect").append("<h1>PV Power</h1><svg>");
			renderPVPowerChart(html);
			html.append("</svg></div>");

			html.div("sect").append("<h1>Load</h1><svg>");
			html.append("</svg></div>");

			html.append(getClass(), "bottom.html");

			try (FileOutputStream fos = new FileOutputStream(target)) {
				fos.write(html.toString().getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}

	private void renderPVPowerChart(HTMLBuilder html) {
		//
		// to avoid ugly aliasing we'll snap to a point in future
		//
		long cur = System.currentTimeMillis() / (1000 * 60 * 10);
		cur++;
		cur *= 1000 * 60 * 10;		
		
		long limit = cur - 1 * 60 * 60 * 1000;
		QueryResults res = datalogger.query(limit, cur, 60, list("PV_TOTAL_P", "PV1_P", "PV2_P", "PV3_P", "PV4_P"));
		System.out.println(res.toString());
	}

	private static List<String> list(String... strings) {
		List<String> a = new ArrayList<>();
		for (String s : strings)
			a.add(s);
		return a;
	}

}
