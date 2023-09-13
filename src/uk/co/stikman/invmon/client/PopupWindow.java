package uk.co.stikman.invmon.client;

import org.teavm.jso.dom.html.HTMLElement;

public class PopupWindow {

	private HTMLElement	root;
	private HTMLElement	window;

	
	public PopupWindow() {
		root = InvMon.div("modalglass");
		window = InvMon.div("content");
		root.appendChild(window);
	}
	
	public PopupWindow(int w, int h) {
		this();
		window.getStyle().setProperty("width", Integer.toString(w));
		window.getStyle().setProperty("height", Integer.toString(h));
	}

	public void showModal() {
		InvMon.getDocument().getBody().appendChild(root);
	}

	public void close() {
		InvMon.getDocument().getBody().removeChild(root);
	}

	public HTMLElement getContent() {
		return window;
	}

}
