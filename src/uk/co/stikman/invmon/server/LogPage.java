package uk.co.stikman.invmon.server;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.client.res.ClientRes;

public class LogPage {

	private UserSesh	sesh;
	private Env			env;

	public LogPage(Env env, UserSesh sesh) {
		this.env = env;
		this.sesh = sesh;
	}

	public String exec() {
		try {
			String html = ClientRes.get("logtemplate.html").toString();

			HTMLBuilder sb = new HTMLBuilder();
			env.getLog().forEach(s -> sb.append("<div class=\"line\">").escape(s).append("</div>"));
			return html.replace("$LOG$", sb.toString());

		} catch (Exception e) {
			return e.toString();
		}

	}

}
