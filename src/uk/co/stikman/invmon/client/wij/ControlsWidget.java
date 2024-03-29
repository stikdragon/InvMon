package uk.co.stikman.invmon.client.wij;

import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.client.AbstractPageWidget;
import uk.co.stikman.invmon.client.Button;
import uk.co.stikman.invmon.client.StandardFrame;
import uk.co.stikman.invmon.client.StandardPage;
import uk.co.stikman.invmon.client.ToggleButton;

public class ControlsWidget extends AbstractPageWidget {

	private StandardFrame	frame;
	private String			name;
	private StandardPage	owner;

	public ControlsWidget(StandardPage owner) {
		super(owner);
		this.owner = owner;
	}

	@Override
	protected void refresh(boolean nomask) {
	}

	@Override
	protected void construct(HTMLElement parent) {
		frame = createStandardFrame(parent, true, "controlswidget");

		Button btn = new Button("Refresh");
		btn.setId("refresh");
		btn.setOnClick(b -> getOwner().getBus().fire(Events.REFRESH_NOW, null));
		frame.content.appendChild(btn.getElement());

		ToggleButton tog = new ToggleButton("Auto Refresh", "unticked.png", "ticked.png");
		tog.setId("auto");
		frame.content.appendChild(tog.getElement());
		tog.setOnClick(b -> {
			getOwner().getBus().fire(Events.AUTOREFRESH_CHANGED, Boolean.valueOf(tog.getState()));
		});

		UserWidget x = new UserWidget(getOwner());
		frame.content.appendChild(x.getElement());
	}

	@Override
	public void configure(JSONObject obj) {
		super.configure(obj);
		this.name = obj.getString("title");
	}

}
