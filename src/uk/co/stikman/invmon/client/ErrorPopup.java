package uk.co.stikman.invmon.client;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

public class ErrorPopup {

	private HTMLElement root;
	private HTMLElement content;

	public ErrorPopup() {
		root = InvMon.div("errorpopup");
		HTMLElement el = InvMon.div("title");
		el.setInnerText("Errors:");
		root.appendChild(el);
		content = InvMon.div("content");
		root.appendChild(content);
	}

	public void show() {
		Window.current().getDocument().getBody().appendChild(root);
	}

	public void hide() {
		Window.current().getDocument().getBody().removeChild(root);
	}

	public void addMessage(String msg) {
		HTMLElement x = InvMon.div("msg");
		x.setInnerText(msg);
		content.appendChild(x);
	}
}
