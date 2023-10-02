package uk.co.stikman.invmon.client;

import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Node;

import uk.co.stikman.invmon.Events;

public class InverterControlWidgetStik extends AbstractPageWidget {

	private StandardFrame	frame;
	private String			name;
	private HTMLElement		txt;
	private String id;

	public InverterControlWidgetStik(ClientPage owner) {
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
		frame = createStandardFrame(parent, true, "sysctrlstik");
		txt = InvMon.text("-");
		txt.getClassList().add("txt");
		frame.content.appendChild(txt);

		frame.content.appendChild(InvMon.div("fill"));
		frame.content.appendChild(InvMon.text2("h3", "Boost:"));

		HTMLElement div = InvMon.div("btns");
		frame.content.appendChild(div);
		div.appendChild(boostButton("1hr", 60).getElement());
		div.appendChild(boostButton("2hr", 120).getElement());
		div.appendChild(boostButton("4hr", 240).getElement());
	}

	private Button boostButton(String title, int mins) {
		Button b = new Button(title);
		b.setOnClick(x -> {
			getOwner().post("stikCtrlBoost", new JSONObject().put("id", id).put("duration", mins), resp -> getOwner().getBus().fire(Events.REFRESH_NOW, null));
		});
		return b;
	}

	@Override
	public void configure(JSONObject obj) {
		super.configure(obj);
		this.name = obj.getString("title");
		this.id = obj.getString("id");
	}

}
