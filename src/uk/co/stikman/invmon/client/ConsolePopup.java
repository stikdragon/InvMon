package uk.co.stikman.invmon.client;

import java.util.function.Consumer;

import org.json.JSONObject;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.xml.Node;

public class ConsolePopup extends PopupWindow {
	private ClientPage			owner;
	private HTMLInputElement	inputline;
	private HTMLElement			container;

	public ConsolePopup(ClientPage owner, Consumer<ConsolePopup> onclose) {
		super();
		this.owner = owner;

		getContent().getClassList().add("consoledialog");

		container = InvMon.div("console");
		inputline = InvMon.element("input");
		inputline.setType("text");
		inputline.addEventListener("keyup", ev -> keyUp(ev.cast()));

		HTMLElement top = InvMon.div("topbar");
		top.appendChild(InvMon.text("Console (type \"help\" for help)"));
		top.appendChild(InvMon.div("fill"));
		HTMLImageElement btn = InvMon.element("img", "close");
		btn.setSrc("close.png");
		top.appendChild(btn);
		btn.addEventListener("click", ev -> {
			onclose.accept(this);
			close();
		});

		getContent().appendChild(top);
		getContent().appendChild(container);
		getContent().appendChild(inputline);
	}

	private void keyUp(KeyboardEvent ev) {
		if (ev.getKey().equals("Enter")) {
			//
			// send it
			//
			JSONObject req = new JSONObject();
			req.put("cmd", inputline.getValue());
			inputline.setValue("");
			owner.post("console", req, resp -> {
				if (resp.getString("status").equals("ok")) {
					container.appendChild(InvMon.text(resp.getString("result"), "result"));
				} else if (resp.getString("status").equals("error")) {
					HTMLElement txt = InvMon.text(resp.getString("error"), "result");
					txt.getClassList().add("error");
					container.appendChild(txt);
				}
				container.setScrollTop(container.getScrollHeight());
			});

		}
	}

	@Override
	public void showModal() {
		super.showModal();
		inputline.focus();

	}
}
