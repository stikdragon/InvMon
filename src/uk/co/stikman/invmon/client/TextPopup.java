package uk.co.stikman.invmon.client;

import org.teavm.jso.dom.html.HTMLElement;

public class TextPopup extends PopupWindow {

	private HTMLElement	txt;
	private HTMLElement	box;

	public TextPopup(ClientPage owner, String log) {
		super(800, 600);
		getContent().getClassList().add("textpopup");

		box = InvMon.div("outer");
		getContent().appendChild(box);

		txt = InvMon.text("-");
		txt.getClassList().add("txt");
		txt.getClassList().add("ctrl_sunk");
		txt.setTextContent(log);
		box.appendChild(txt);

		Button btnClose = new Button("Close");

		box.appendChild(txt);
		box.appendChild(btnClose.getElement());
		btnClose.setOnClick(ev -> close());

	}

}
