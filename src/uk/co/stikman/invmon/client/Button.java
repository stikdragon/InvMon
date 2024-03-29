package uk.co.stikman.invmon.client;

import java.util.function.Consumer;

import org.teavm.jso.dom.html.HTMLElement;

public class Button {

	private HTMLElement	root;
	private String		caption;
	private HTMLElement	inner;

	public Button(String caption) {
		this(caption, null);
	}

	public Button(String caption, Consumer<Button> onclick) {
		this.caption = caption;
		this.root = InvMon.div("button");
		inner = InvMon.div("content");
		inner.setTextContent(caption);
		root.appendChild(inner);
		if (onclick != null)
			setOnClick(onclick);
	}

	public void addStyleClass(String cls) {
		root.getClassList().add(cls);
	}

	public HTMLElement getElement() {
		return root;
	}

	public void setId(String id) {
		root.setId(id);
	}

	public void setOnClick(Consumer<Button> onclick) {
		root.addEventListener("click", ev -> onclick.accept(this));
	}

}
