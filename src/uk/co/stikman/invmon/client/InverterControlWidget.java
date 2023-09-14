package uk.co.stikman.invmon.client;

import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;

public class InverterControlWidget extends AbstractPageWidget {

	private StandardFrame	frame;
	private String			name;
	private HTMLElement		txt;

	public InverterControlWidget(ClientPage owner) {
		super(owner);
	}

	@Override
	protected void refresh(boolean nomask) {
		JSONObject args = new JSONObject();
		args.put("name", getId());
		if (!nomask)
			frame.showGlass();
		getOwner().fetch("executeChart", args, result -> {
			txt.setTextContent(result.getString("summary"));
			frame.hideOverlays();
		}, err -> {
			frame.showError(err.getMessage());
		});
	}

	@Override
	protected void construct(HTMLElement parent) {
		frame = createStandardFrame(parent, true, "invertercontrolswidget");
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
