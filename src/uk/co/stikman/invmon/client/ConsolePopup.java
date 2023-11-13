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

public class ConsolePopup extends PopupWindow {
	private static final String STORAGE_KEY = "console-history";
	private ClientPage			owner;
	private HTMLInputElement	inputline;
	private HTMLElement			container;
	private HTMLElement			lblModule;
	private int					historyIndex;
	private List<String>		history	= new ArrayList<>();

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
					container.appendChild(InvMon.text(resp.getString("result"), "result"));
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
		String cmd =  historyIndex >= history.size() ? "" : history.get(historyIndex);
		inputline.setValue(cmd);
	}

	@Override
	public void showModal() {
		super.showModal();
		inputline.focus();
	}
}
