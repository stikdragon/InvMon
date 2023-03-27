package uk.co.stikman.invmon.client;

import org.json.JSONArray;
import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.htmlout.ChartOptions;

public class ChartWidget extends AbstractPageWidget {

	private StandardFrame	frame;
	private String			name;

	public ChartWidget(ClientPage owner) {
		super(owner);
	}

	@Override
	protected void refresh(boolean nomask) {
		JSONObject args = new JSONObject();
		args.put("name", getId());
		int w = frame.content.getOffsetWidth() - 20;
		int h = frame.content.getOffsetHeight() - 20;
		args.put("w", w); // padding is 20... TODO: work it out properly
		args.put("h", h);
		if (!nomask)
			frame.showGlass();
		getOwner().fetch("executeChart", args, result -> {
			ChartOptions opts = new ChartOptions();
			opts.fromJSON(result.getJSONObject("config"));
			
			frame.content.clear();
			HTMLElement div = InvMon.div();
			div.setInnerHTML(result.getString("html"));
			div.getStyle().setProperty("position", "relative");
			frame.content.appendChild(div);

			GraphHoverThing thing = new GraphHoverThing(opts.getAxisY1().getSize(), 0, w - opts.getAxisY1().axisSize() - opts.getAxisY2().axisSize(), h - opts.getAxisX1().getSize(), opts);
			div.appendChild(thing.getElement());

			JSONArray arr = result.optJSONArray("titleBits");
			frame.header.clear();
			HTMLElement h1 = InvMon.element("h1", "title");
			h1.setInnerText(name);
			frame.header.appendChild(h1);
			if (arr != null) {
				for (int i = 0; i < arr.length(); ++i) {
					div = InvMon.div("grp");
					frame.header.appendChild(div);
					div.setInnerHTML(arr.getString(i));
				}
			}
			frame.hideOverlays();
		}, err -> {
			frame.showError(err.getMessage());
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
