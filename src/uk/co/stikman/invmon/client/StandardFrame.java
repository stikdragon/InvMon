package uk.co.stikman.invmon.client;

import org.teavm.jso.dom.html.HTMLElement;

public class StandardFrame {
	public HTMLElement	header;
	public HTMLElement	content;
	public HTMLElement	glass;
	public HTMLElement	error;

	public void showError(String msg) {
		if (error == null)
			return;
		error.getStyle().removeProperty("display");
		error.getFirstChild().setTextContent(msg);
	}

	public void hideOverlays() {
		if (glass != null)
			glass.getStyle().setProperty("display", "none");
		if (error != null)
			error.getStyle().setProperty("display", "none");
	}

	public void showGlass() {
		if (glass == null)
			return;
		glass.getStyle().removeProperty("display");
	}

}
