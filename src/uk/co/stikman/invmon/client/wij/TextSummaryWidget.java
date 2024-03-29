package uk.co.stikman.invmon.client.wij;

import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.client.AbstractPageWidget;
import uk.co.stikman.invmon.client.ClientPage;
import uk.co.stikman.invmon.client.InvMon;
import uk.co.stikman.invmon.client.StandardFrame;

public class TextSummaryWidget extends AbstractPageWidget {
	private StandardFrame	frame;
	private String			title;
	private HTMLElement		txt;

	public TextSummaryWidget(ClientPage owner) {
		super(owner);
	}

	@Override
	protected void refresh(boolean nomask) {
		JSONObject args = new JSONObject();
		args.put("name", getId());
		if (!nomask)
			frame.showGlass();
		api("execute", args, result -> {
			txt.setTextContent(result.getString("summary"));
			frame.hideOverlays();
		}, err -> {
			frame.showError(err.getMessage());
		});
	}

	@Override
	protected void construct(HTMLElement parent) {
		frame = createStandardFrame(parent, true, "text-summary");
		frame.setTitle(title);
		txt = InvMon.text("-");
		txt.getClassList().add("txt");
		frame.content.appendChild(txt);
	}

	@Override
	public void configure(JSONObject obj) {
		super.configure(obj);
		this.title = obj.getString("title");
	}
}
