package uk.co.stikman.invmon.client;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Element;
import org.teavm.jso.dom.xml.Node;

public class InvMon {

	public static InvMon		INSTANCE;

	private static Window		browserWindow;
	private static HTMLDocument	doc;
	private LoggedInUser		user;
	private static ErrorPopup	errorPopup;

	//
	// just a couple of shortcuts
	//
	static {
		browserWindow = Window.current();
		doc = browserWindow.getDocument();
	}

	public static void main(String[] args) {
		INSTANCE = new InvMon();
		INSTANCE.go();
	}

	public static HTMLElement div(String... classes) {
		return element("div", classes);
	}

	@SuppressWarnings("unchecked")
	public static <T extends HTMLElement> T element(String type, String... classes) {
		HTMLElement el = getDocument().createElement(type);
		for (String s : classes)
			el.getClassList().add(s);
		return (T) el;
	}

	public static HTMLDocument getDocument() {
		return doc;
	}

	public void go() {
		StandardPage mainpage = new StandardPage();
		getDocument().getBody().appendChild(mainpage.getElement());
		mainpage.load();
	}

	public static ErrorPopup getErrorPopup() {
		if (errorPopup == null)
			errorPopup = new ErrorPopup();
		return errorPopup;
	}

	public static Element createSvgElement(String type) {
		return getDocument().createElementNS("http://www.w3.org/2000/svg", type);
	}

	public LoggedInUser getUser() {
		return user;
	}

	public void setUser(LoggedInUser usr) {
		this.user = usr;
	}

	public static HTMLElement text2(String element, String text) {
		HTMLElement el = element(element);
		el.setTextContent(text);
		return el;
	}

	public static HTMLElement text(String text) {
		HTMLElement el = div();
		el.setTextContent(text);
		return el;
	}

	public static HTMLElement text(String text, String csscls) {
		HTMLElement el = text(text);
		el.getClassList().add(csscls);
		return el;
	}

	public void mask() {
		// TODO: mask
	}

	public void unmask() {
		// TODO mask
		
	}
}
