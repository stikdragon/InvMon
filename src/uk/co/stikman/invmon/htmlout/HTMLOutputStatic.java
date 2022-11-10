package uk.co.stikman.invmon.htmlout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.inverter.InvUtil;
import uk.co.stikman.log.StikLog;

public class HTMLOutputStatic extends InvModule {
	private static final StikLog	LOGGER	= StikLog.getLogger(HTMLOutputStatic.class);
	private File					target;
	private long					lastT;
	private DataLogger				datalogger;
	private static final String[]	COLOURS	= new String[] { "#ff7c7c", "#7cff7c", "#7c7cff", "#ff7cff" };

	public HTMLOutputStatic(String id, Env env) {
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
		if (dt > 5000) {
			HTMLBuilder html = new HTMLBuilder();
			new HTMLGenerator(datalogger).render(html, data);

			try (FileOutputStream fos = new FileOutputStream(target)) {
				fos.write(html.toString().getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}

}
