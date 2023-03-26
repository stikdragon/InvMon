package uk.co.stikman.invmon.client;

import java.util.function.Consumer;

import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLImageElement;

public class ToggleButton {

	private HTMLElement				root;
	private String					caption;
	private HTMLElement				inner;
	private HTMLImageElement		img;
	private String					imageOn;
	private String					imageOff;
	private Consumer<ToggleButton>	onClick;
	private boolean					state	= false;

	public ToggleButton(String caption) {
		this(caption, null, null);
	}

	public ToggleButton(String caption, String imageOff, String imageOn) {
		this.caption = caption;
		this.root = InvMon.div("togglebutton");
		this.imageOn = imageOn;
		this.imageOff = imageOff;
		if (imageOff != null) {
			img = InvMon.element("img");
			root.appendChild(img);
		}

		root.addEventListener("click", ev -> {
			setState(!state);
			if (onClick != null)
				onClick.accept(this);
		});

		setState(false);

		inner = InvMon.div("content");
		inner.setTextContent(caption);
		root.appendChild(inner);
	}

	public HTMLElement getElement() {
		return root;
	}

	public void setId(String id) {
		root.setId(id);
	}

	public void setOnClick(Consumer<ToggleButton> onclick) {
		onClick = onclick;
	}

	public boolean getState() {
		return state;
	}

	public void setState(boolean state) {
		this.state = state;
		if (state)
			root.getClassList().add("selected");
		else
			root.getClassList().remove("selected");
		img.setSrc(state ? imageOn : imageOff);
	}

}
