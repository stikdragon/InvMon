package uk.co.stikman.invmon.client;

import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.client.AbstractPageWidget;
import uk.co.stikman.invmon.client.Button;
import uk.co.stikman.invmon.client.ClientPage;
import uk.co.stikman.invmon.client.InvMon;
import uk.co.stikman.invmon.client.StandardFrame;
import uk.co.stikman.invmon.client.StikBoostMenu;

public class InverterControlWidgetStik extends AbstractPageWidget {

	private StandardFrame	frame;
	private HTMLElement		txt;

	public InverterControlWidgetStik(ClientPage owner) {
		super(owner);
	}

	@Override
	protected void refresh(boolean nomask) {
		if (!nomask)
			frame.showGlass();
		api("summary", null, result -> {
			txt.setTextContent(result.getString("summary"));
			frame.hideOverlays();
		}, err -> {
			frame.showError(err.getMessage());
		});
	}

	@Override
	protected void construct(HTMLElement parent) {
		frame = createStandardFrame(parent, true, "sysctrlstik");
		txt = InvMon.text("-");
		txt.getClassList().add("txt");
		frame.content.appendChild(txt);

		frame.content.appendChild(InvMon.div("fill"));
		frame.content.appendChild(InvMon.text2("h3", "Boost:"));

		Button b = new Button("+");
		b.addStyleClass("mini");
		b.setOnClick(x -> {
			showBoostMenu();
		});
		frame.content.appendChild(b.getElement());

	}

	private void showBoostMenu() {
		StikBoostMenu dlg = new StikBoostMenu(this.getOwner(), dur -> {
			api("setBoost", new JSONObject().put("duration", dur.intValue()), resp -> getOwner().getBus().fire(Events.REFRESH_NOW, null));
		});
		dlg.showModal();
	}

	@Override
	public void configure(JSONObject obj) {
		super.configure(obj);
	}

}
