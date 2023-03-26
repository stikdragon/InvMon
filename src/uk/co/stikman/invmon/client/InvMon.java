package uk.co.stikman.invmon.client;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

public class InvMon {

	public static InvMon		INSTANCE;

	private static Window		browserWindow;
	private static HTMLDocument	doc;

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
		MainPage mainpage = new MainPage();
		getDocument().getDocumentElement().appendChild(mainpage.getElement());
		mainpage.load();
	}

	public static ErrorPopup getErrorPopup() {
		if (errorPopup == null)
			errorPopup = new ErrorPopup();
		return errorPopup;
	}
}
