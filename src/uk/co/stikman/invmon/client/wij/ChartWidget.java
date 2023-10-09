package uk.co.stikman.invmon.client.wij;

import org.json.JSONArray;
import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.client.AbstractPageWidget;
import uk.co.stikman.invmon.client.ClientPage;
import uk.co.stikman.invmon.client.DataSetImpl;
import uk.co.stikman.invmon.client.InvMon;
import uk.co.stikman.invmon.client.StandardFrame;
import uk.co.stikman.invmon.server.ChartOptions;
import uk.co.stikman.invmon.server.HTMLBuilder;
import uk.co.stikman.invmon.server.HTMLGenerator;

public class ChartWidget extends AbstractPageWidget {

	private StandardFrame frame;

	public ChartWidget(ClientPage owner) {
		super(owner);
	}

	@Override
	protected void refresh(boolean nomask) {
		JSONObject args = new JSONObject();
		int w = frame.content.getOffsetWidth() - 20;
		int h = frame.content.getOffsetHeight() - 20;
		args.put("w", w); // padding is 20... TODO: work it out properly
		args.put("h", h);
		if (!nomask)
			frame.showGlass();

		api("execute", args, result -> {
			ChartOptions opts = new ChartOptions();
			opts.fromJSON(result.getJSONObject("config"));

			DataSetImpl data = new DataSetImpl();
			data.fromJSON(result.getJSONObject("data"));
			HTMLBuilder html = new HTMLBuilder();
			HTMLGenerator.renderChart(html, result.getString("css"), opts, data);

			frame.content.clear();
			HTMLElement div = InvMon.div();
			div.setInnerHTML(html.toString());
			div.getStyle().setProperty("position", "relative");
			frame.content.appendChild(div);

			GraphHoverThing thing = new GraphHoverThing(opts.getAxisY1().getSize(), 0, w - opts.getAxisY1().axisSize() - opts.getAxisY2().axisSize(), h - opts.getAxisX1().getSize(), opts);
			div.appendChild(thing.getElement());

			JSONArray arr = result.optJSONArray("titleBits");
			frame.clearExtraHeaderBits();
			if (arr != null) {
				for (int i = 0; i < arr.length(); ++i) {
					div = InvMon.div("grp");
					frame.appendExtraHeaderBit(div);
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
	}

}
