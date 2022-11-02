package uk.co.stikman.invmon.htmlout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.HTMLConsoleThing;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.ProcessPart;
import uk.co.stikman.invmon.TempHTMLOutput;
import uk.co.stikman.invmon.inverter.InvUtil;
import uk.co.stikman.log.StikLog;

public class HTMLOutput extends ProcessPart {
	private static final StikLog	LOGGER	= StikLog.getLogger(HTMLOutput.class);
	private File					target;
	private long					lastT;

	public HTMLOutput(String id, Env env) {
		super(id, env);
	}

	@Override
	public void configure(Element config) {
		this.target = new File(InvUtil.getAttrib(config, "target"));
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
			
			
			html.append(getClass(), "bottom.html");
			
			
			try (FileOutputStream fos = new FileOutputStream(target)) {
				fos.write(html.toString().getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}

}
