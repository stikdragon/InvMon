package uk.co.stikman.invmon.client;

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLElement;

public class Menu {

	private HTMLElement			root;
	private EventListener<?>	dch;

	public Menu() {
		super();
		this.root = InvMon.div("Menu");
	}

	public void addItem(String title, Runnable onClick) {
		HTMLElement div = InvMon.div("MenuItem");
		div.setTextContent(title);
		div.addEventListener("click", ev -> {
			hide();
			onClick.run();
		});
		root.appendChild(div);
	}

	@JSBody(params = { "a", "b" }, script = "return a.contains(b);")
	public static native boolean contains(HTMLElement a, HTMLElement b);

	public void showAt(int x, int y) {
		InvMon.getDocument().getBody().appendChild(root);
		root.getStyle().setProperty("left", x + "px");
		root.getStyle().setProperty("top", y + "px");
		dch = ev -> {
			System.out.println("click");
			if (!contains(root, (HTMLElement) ev.getTarget())) {
				hide();
			}
		};
		InvMon.getDocument().addEventListener("mousedown", dch); // why doesn't this capture right click???
	}

	public void hide() {
		if (dch != null)
			InvMon.getDocument().removeEventListener("mousedown", dch);
		dch = null;
		InvMon.getDocument().getBody().removeChild(root);
	}
}
