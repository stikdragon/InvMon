package uk.co.stikman.invmon.client;

import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.Events;

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
		btn.setOnClick(b -> ((MainPage) getOwner()).refresh());
		frame.content.appendChild(btn.getElement());

		ToggleButton tog = new ToggleButton("Auto Refresh", "unticked.png", "ticked.png");
		tog.setId("auto");
		frame.content.appendChild(tog.getElement());
		tog.setOnClick(b -> {
			getOwner().getBus().fire(Events.AUTOREFRESH_CHANGED, Boolean.valueOf(tog.getState()));
		});
	}

	@Override
	public void configure(JSONObject obj) {
		super.configure(obj);
		this.name = obj.getString("title");
	}

}
