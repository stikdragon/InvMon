package uk.co.stikman.invmon.client;

import org.json.JSONObject;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.Events;

public class ControlsWidget extends AbstractPageWidget {

	private StandardFrame	frame;
	private String			name;

	public ControlsWidget(ClientPage owner) {
		super(owner);
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
		frame.appendExtraHeaderBit(createOptionsButton());
	}

	private HTMLElement createOptionsButton() {
		HTMLElement el = InvMon.div("options");
		el.addEventListener("click", ev -> {
			ev.stopPropagation();
			MouseEvent ev2 = (MouseEvent) ev;
			Menu mnu = new Menu();
			mnu.addItem("Login", null);
			mnu.addItem("Show log..", this::showLogWindow);
			mnu.showAt(ev2.getClientX(), ev2.getClientY());
		});
		return el;
	}

	@Override
	public void configure(JSONObject obj) {
		super.configure(obj);
		this.name = obj.getString("title");
	}

	private void showLogWindow() {
		getOwner().fetch("fetchLog", new JSONObject(), result -> {
			LogPopup wnd = new LogPopup(getOwner(), result.getString("log"));
			wnd.showModal();
		}, err -> {
			
		});
	}

}
