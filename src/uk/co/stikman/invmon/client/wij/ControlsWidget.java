package uk.co.stikman.invmon.client.wij;

import org.json.JSONObject;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.client.AbstractPageWidget;
import uk.co.stikman.invmon.client.Button;
import uk.co.stikman.invmon.client.ClientPage;
import uk.co.stikman.invmon.client.InvMon;
import uk.co.stikman.invmon.client.StandardPage;
import uk.co.stikman.invmon.client.TextPopup;
import uk.co.stikman.invmon.client.Menu;
import uk.co.stikman.invmon.client.StandardFrame;
import uk.co.stikman.invmon.client.ToggleButton;

public class ControlsWidget extends AbstractPageWidget {

	private StandardFrame	frame;
	private String			name;
	private StandardPage		owner;

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
		frame.appendExtraHeaderBit(createOptionsButton());
	}

	private HTMLElement createOptionsButton() {
		HTMLElement el = InvMon.div("options");
		el.addEventListener("click", ev -> {
			ev.stopPropagation();
			MouseEvent ev2 = (MouseEvent) ev;
			Menu mnu = new Menu();
			mnu.addItem("Login", () -> getOwner().doLogin());
			mnu.addItem("Show log..", this::showLogWindow);
			mnu.addItem("Show layout", this::showLayout);
			mnu.addItem("Console..", this::showConsole);
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
		getOwner().fetch("fetchUserLog", new JSONObject(), result -> {
			TextPopup wnd = new TextPopup(getOwner(), result.getString("log"));
			wnd.showModal();
		}, err -> {

		});
	}

	private void showConsole() {
		owner.showConsole();
	}
	
	private void showLayout() {
		int gs = owner.getGridSize();
		StringBuilder sb = new StringBuilder();
		for (AbstractPageWidget w : owner.getWidgets()) {
			sb.append(w.getId()).append(": ").append(w.getName()).append(" - ");
			sb.append((int) (w.getX() / gs)).append(", ");
			sb.append((int) (w.getY() / gs)).append(", ");
			sb.append((int) (w.getWidth() / gs)).append(", ");
			sb.append((int) (w.getHeight() / gs)).append("\n");
		}
		TextPopup wnd = new TextPopup(getOwner(), sb.toString());
		wnd.showModal();
	}

}
