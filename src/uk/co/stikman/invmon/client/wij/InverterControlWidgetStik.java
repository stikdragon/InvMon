package uk.co.stikman.invmon.client.wij;

import java.util.function.Consumer;

import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.client.AbstractPageWidget;
import uk.co.stikman.invmon.client.Button;
import uk.co.stikman.invmon.client.ClientPage;
import uk.co.stikman.invmon.client.InvMon;
import uk.co.stikman.invmon.client.StandardFrame;

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

		Button b = new Button("+");
		b.addStyleClass("mini");
		b.setOnClick(x -> {
			showBoostMenu();
		});
		frame.content.appendChild(b.getElement());

		HTMLElement div = InvMon.div("horiz");
		frame.content.appendChild(div);
		div.appendChild(InvMon.text("Charge:", "txt"));
		b = new Button("On", x -> forceCharge(true));
		b.addStyleClass("mini");
		div.appendChild(b.getElement());
		b = new Button("Off", x -> forceCharge(false));
		b.addStyleClass("mini");
		div.appendChild(b.getElement());
	}

	private void forceCharge(boolean b) {
		api("forceCharge", new JSONObject().put("state", b), resp -> getOwner().getBus().fire(Events.REFRESH_NOW, null));
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
