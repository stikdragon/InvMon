package uk.co.stikman.invmon.client;

import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * this expects the server to render something as HTML
 * 
 * @author stik
 *
 */
public class ServerRenderedWidget extends AbstractPageWidget {

	private StandardFrame	frame;
	private String			name;

	public ServerRenderedWidget(ClientPage owner) {
		super(owner);
	}

	@Override
	protected void refresh(boolean nomask) {
		JSONObject args = new JSONObject();
		int w = frame.content.getOffsetWidth() - 20;
		int h = frame.content.getOffsetHeight() - 20;
		args.put("w", w); // padding is 20... TODO: work it out properly
		args.put("h", h);
		if (!nomask)
			frame.showGlass();
		api("execute", args, result -> {
			frame.content.setInnerHTML(result.getString("html"));
			frame.hideOverlays();
		}, err -> {
			frame.showError(err.getMessage());
		});
	}

	@Override
	protected void construct(HTMLElement parent) {
		frame = createStandardFrame(parent, true, null);
	}

	@Override
	public void configure(JSONObject obj) {
		super.configure(obj);
		this.name = obj.getString("title");
	}

}
