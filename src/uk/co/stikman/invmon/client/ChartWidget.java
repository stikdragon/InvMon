package uk.co.stikman.invmon.client;

import org.json.JSONArray;
import org.json.JSONObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

public class ChartWidget extends AbstractPageWidget {

	private StandardFrame	frame;
	private String			name;

	public ChartWidget(ClientPage owner) {
		super(owner);
	}

	@Override
	protected void refresh() {
		JSONObject args = new JSONObject();
		args.put("name", getId());
		args.put("w", frame.content.getOffsetWidth() - 20); // padding is 20... TODO: work it out properly
		args.put("h", frame.content.getOffsetHeight() - 20);
		frame.showGlass();
		getOwner().fetch("getSectData", args, result -> {
			frame.content.setInnerHTML(result.getString("contentHtml"));
			JSONArray arr = result.getJSONArray("titleBits");
			frame.header.clear();
			HTMLElement h1 = InvMon.element("h1", "title");
			h1.setInnerText(name);
			frame.header.appendChild(h1);
			for (int i = 0; i < arr.length(); ++i) {
				HTMLElement div = InvMon.div("grp");
				frame.header.appendChild(div);
				div.setInnerHTML(arr.getString(i));
			}
			frame.hideOverlays();
		});
	}

	@Override
	protected void construct(HTMLElement parent) {
		frame = createStandardFrame(parent, true, null);
	}

	@Override
	public void configure(JSONObject obj) {
		super.configure(obj);
		this.name = obj.getString("title");
	}

}
