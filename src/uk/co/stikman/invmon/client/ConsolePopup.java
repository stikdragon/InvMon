package uk.co.stikman.invmon.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.xml.Element;
import org.teavm.jso.dom.xml.Node;

public class ConsolePopup extends PopupWindow {
	private static final String	STORAGE_KEY	= "console-history";
	private ClientPage			owner;
	private HTMLInputElement	inputline;
	private HTMLElement			container;
	private HTMLElement			lblModule;
	private int					historyIndex;
	private List<String>		history		= new ArrayList<>();

	public ConsolePopup(ClientPage owner, Consumer<ConsolePopup> onclose) {
		super();
		this.owner = owner;

		getContent().getClassList().add("consoledialog");

		container = InvMon.div("console");

		HTMLElement top = InvMon.div("topbar");
		top.appendChild(InvMon.text("Console (type \"help\" for help)"));
		top.appendChild(InvMon.div("fill"));
		HTMLImageElement btn = InvMon.element("img", "close");
		btn.setSrc("close.png");
		top.appendChild(btn);
		btn.addEventListener("click", ev -> {
			if (onclose != null)
				onclose.accept(this);
			close();
		});

		HTMLElement bottom = InvMon.div("bottom");
		inputline = InvMon.element("input");
		inputline.setType("text");
		inputline.addEventListener("keyup", ev -> keyUp(ev.cast()));

		lblModule = InvMon.div("module");
		lblModule.setTextContent("");
		HTMLElement d2 = InvMon.div("x1");
		d2.appendChild(lblModule);

		bottom.appendChild(d2);
		bottom.appendChild(inputline);

		getContent().appendChild(top);
		getContent().appendChild(container);
		getContent().appendChild(bottom);

		String s = ClientUtil.getLocalStorageItem(STORAGE_KEY);
		if (s != null) {
			JSONArray arr = new JSONArray(s);
			for (int i = 0; i < arr.length(); ++i)
				history.add(arr.getString(i));
			historyIndex = history.size();
		}

	}

	private void keyUp(KeyboardEvent ev) {
		if (ev.getCode().equals("ArrowUp")) {
			selectHistoryCommand(-1);
			ev.preventDefault();
		} else if (ev.getCode().equals("ArrowDown")) {
			selectHistoryCommand(1);
			ev.preventDefault();
		}
		if (ev.getKey().equals("Enter")) {
			//
			// send it
			//
			JSONObject req = new JSONObject();
			req.put("cmd", inputline.getValue());
			addHistoryItem(inputline.getValue());

			HTMLElement sent = InvMon.text(lblModule.getTextContent() + " > " + inputline.getValue(), "result");
			sent.getClassList().add("sent");
			container.appendChild(sent);

			inputline.setValue("");
			owner.post("console", req, resp -> {
				if (resp.getString("status").equals("ok")) {
					container.appendChild(createResponseDiv(resp.getString("result"), resp.optBoolean("formatted")));
					if (resp.has("module"))
						lblModule.setTextContent(resp.getString("module"));
				} else if (resp.getString("status").equals("error")) {
					HTMLElement txt = InvMon.text(resp.getString("error"), "result");
					txt.getClassList().add("error");
					container.appendChild(txt);
				}
				container.setScrollTop(container.getScrollHeight());
			});
			ev.preventDefault();
		}
	}

	
	private HTMLElement createResponseDiv(String text, boolean formatted) {
		if (!formatted)
			return InvMon.text(text, "result");

		//
		// formatted one supports five style classes, sty1-5
		// you specify them with ^1, ^2, ^3, etc, or ^x to reset
		// a literal ^ is put in by doing ^^
		//
		HTMLElement root = InvMon.div("result", "formatted-text");
		CharIter iter = new CharIter(text);
		StringBuilder sb = new StringBuilder();
		int colour = 1;
		while (iter.hasNext()) {
			char ch = iter.next();
			if (ch == '^') {
				char next = iter.peek();
				if (next == '^') {
					sb.append("^");
					iter.next();
				} else if (next >= '1' && next <= '5') {
					iter.next();
					append(root, colour, sb.toString());
					colour = next - '0';
					sb = new StringBuilder();
				} else if (next == 'x') {
					iter.next();
					append(root, colour, sb.toString());
					colour = 1;
					sb = new StringBuilder();
				}
			} else {
				sb.append(ch);
			}
		}
		append(root, colour, sb.toString());

		return root;
	}

	private void append(HTMLElement root, int colour, String text) {
		if (text.length() == 0)
			return;
		HTMLElement span = InvMon.element("span", "sty" + colour);
		span.setTextContent(text);
		root.appendChild(span);
	}

	private void addHistoryItem(String value) {
		history.add(value);
		historyIndex = history.size();
		JSONArray arr = new JSONArray();
		history.forEach(arr::put);
		ClientUtil.setLocalStorageItem(STORAGE_KEY, arr.toString());
	}

	private void selectHistoryCommand(int d) {
		historyIndex += d;
		if (historyIndex < 0)
			historyIndex = 0;
		if (historyIndex > history.size())
			historyIndex = history.size();
		String cmd = historyIndex >= history.size() ? "" : history.get(historyIndex);
		inputline.setValue(cmd);
	}

	@Override
	public void showModal() {
		super.showModal();
		inputline.focus();
	}
}
