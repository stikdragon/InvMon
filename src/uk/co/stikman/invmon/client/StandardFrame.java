package uk.co.stikman.invmon.client;

import org.teavm.jso.dom.html.HTMLElement;

public class StandardFrame {
	private HTMLElement	header;
	public HTMLElement	content;
	public HTMLElement	glass;
	public HTMLElement	error;
	private HTMLElement	title;

	public StandardFrame(boolean includeHeader) {
		super();
		if (includeHeader) {
			header = InvMon.div("hdr");
			title = InvMon.element("h1", "title");
			header.appendChild(title);
			header.appendChild(InvMon.div("fill"));
		}
	}

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

	public void setTitle(String s) {
		if (title != null)
			title.setInnerText(s);
	}

	public HTMLElement getHeader() {
		return header;
	}

	public void clearExtraHeaderBits() {
		if (header == null)
			return;
		header.clear();
		header.appendChild(title);
		header.appendChild(InvMon.div("fill"));
	}

	public void appendExtraHeaderBit(HTMLElement el) {
		header.appendChild(el);
	}

}
