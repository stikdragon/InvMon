package uk.co.stikman.invmon.client.wij;

import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.client.AbstractPageWidget;
import uk.co.stikman.invmon.client.ClientPage;
import uk.co.stikman.invmon.client.InvMon;
import uk.co.stikman.invmon.client.StandardFrame;

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
		api("fetch", new JSONObject(), res -> {
			txt.setTextContent(res.getString("log"));
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
		txt.getClassList().add("ctrl_sunk");
		frame.content.appendChild(txt);
	}

	@Override
	public void configure(JSONObject obj) {
		super.configure(obj);
		this.name = obj.getString("title");
	}

}
