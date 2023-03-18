package uk.co.stikman.invmon.client;

import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;

public class ControlsWidget extends AbstractPageWidget {

	private StandardFrame	frame;
	private String			name;

	public ControlsWidget(ClientPage owner) {
		super(owner);
	}

	@Override
	protected void refresh() {
	}

	@Override
	protected void construct(HTMLElement parent) {
		frame = createStandardFrame(parent, true, "controlswidget");

		Button btn = new Button("Refresh");
		btn.setId("refresh");
		btn.setOnClick(b -> ((MainPage)getOwner()).refresh());
		frame.content.appendChild(btn.getElement());
	}

	@Override
	public void configure(JSONObject obj) {
		super.configure(obj);
		this.name = obj.getString("title");
	}

}
