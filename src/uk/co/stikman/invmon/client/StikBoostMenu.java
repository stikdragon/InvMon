package uk.co.stikman.invmon.client;

import java.util.function.Consumer;

import org.teavm.jso.dom.html.HTMLElement;

public class StikBoostMenu extends PopupWindow {

	private ClientPage			owner;
	private Consumer<Integer>	select;

	public StikBoostMenu(ClientPage owner, Consumer<Integer> select) {
		super();
		this.owner = owner;
		this.select = select;

		getContent().getClassList().add("boostdialog");
		HTMLElement h2 = InvMon.text2("h2", "Boost");
		getContent().appendChild(h2);

		HTMLElement div = InvMon.div("buttons");
		getContent().appendChild(div);

		div.appendChild(boostButton("1hr", 1 * 60).getElement());
		div.appendChild(boostButton("2hr", 2 * 60).getElement());
		div.appendChild(boostButton("4hr", 4 * 60).getElement());
		div.appendChild(boostButton("6hr", 6 * 60).getElement());
		div.appendChild(boostButton("8hr", 8 * 60).getElement());

		div = InvMon.div("buttons", "reset");
		getContent().appendChild(div);
		div.appendChild(boostButton("Reset", 0).getElement());

		div = InvMon.div("buttons", "cancel");
		getContent().appendChild(div);
		Button b = new Button("Cancel");
		b.setOnClick(x -> close());
		div.appendChild(b.getElement());
	}

	private Button boostButton(String title, int mins) {
		Button b = new Button(title);
		b.setOnClick(x -> {
			select.accept(Integer.valueOf(mins));
			close();
		});
		return b;
	}

}
