package uk.co.stikman.invmon.client;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

public class MessagePopup {
	
	public enum Type {
		MESSAGE,
		WARNING,
		ERROR
	}
	private HTMLElement	root;
	private HTMLElement	content;

	public MessagePopup() {
		root = InvMon.div("messagepopup");
		//		HTMLElement el = InvMon.div("title");
		//		el.setInnerText("Errors:");
		//		root.appendChild(el);
		content = InvMon.div("content");
		root.appendChild(content);
	}

	public void show() {
		Window.current().getDocument().getBody().appendChild(root);
	}

	public void hide() {
		Window.current().getDocument().getBody().removeChild(root);
	}

	public void addMessage(String msg, Type type) {
		HTMLElement x = InvMon.div("msg", type.name().toLowerCase());
		x.setInnerText(msg);
		content.appendChild(x);
		Window.setTimeout(() -> x.getClassList().add("fadeout"), 3500);
		Window.setTimeout(() -> content.removeChild(x), 4000);
		show();
	}
}
