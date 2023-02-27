package uk.co.stikman.invmon.client;

import org.json.JSONArray;
import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;

public class ChartWidget extends PageWidget {

	private StandardFrame frame;

	public ChartWidget(ClientPage owner) {
		super(owner);
	}

	@Override
	protected void refresh() {
		JSONObject args = new JSONObject();
		args.put("name", getId());
		args.put("w", frame.content.getClientWidth());
		args.put("h", frame.content.getClientHeight());
		getOwner().fetch("getSectData", args, result -> {
			frame.content.setInnerHTML(result.getString("contentHtml"));
			JSONArray arr = result.getJSONArray("titleBits");
			frame.header.clear();
			for (int i = 0; i < arr.length(); ++i) {
				HTMLElement div = InvMon.div();
				frame.header.appendChild(div);
				div.setInnerHTML(arr.getString(i));
			}
		});
	}

	@Override
	protected void construct(HTMLElement parent) {
		frame = createStandardFrame(parent, true, "chart");
	}

}
