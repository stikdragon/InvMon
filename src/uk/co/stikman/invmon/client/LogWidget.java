package uk.co.stikman.invmon.client;

import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;

public class LogWidget extends AbstractPageWidget {

	private StandardFrame	frame;
	private String			name;
	private HTMLElement		txt;
	private HTMLElement		outer;

	public LogWidget(ClientPage owner) {
		super(owner);
	}

	@Override
	protected void refresh(boolean nomask) {
		if (!nomask)
			frame.showGlass();
		getOwner().fetch("fetchLog", new JSONObject(), result -> {
			txt.setTextContent(result.getString("log"));
			frame.hideOverlays();
		}, err -> {
			frame.showError(err.getMessage());
		});
	}

	@Override
	protected void construct(HTMLElement parent) {
		frame = createStandardFrame(parent, true, "logwidget");
		outer = InvMon.div("scroller");
		txt = InvMon.text("-");
		txt.getClassList().add("txt");
		frame.content.appendChild(txt);
	}

	@Override
	public void configure(JSONObject obj) {
		super.configure(obj);
		this.name = obj.getString("title");
	}

}
