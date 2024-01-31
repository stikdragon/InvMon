package uk.co.stikman.invmon.client.wij;

import java.text.DecimalFormat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.client.AbstractPageWidget;
import uk.co.stikman.invmon.client.ClientPage;
import uk.co.stikman.invmon.client.InvMon;
import uk.co.stikman.invmon.client.StandardFrame;

public class BMSStatusWidget extends AbstractPageWidget {

	private StandardFrame frame;

	public BMSStatusWidget(ClientPage owner) {
		super(owner);
	}

	@Override
	protected void refresh(boolean nomask) {
		if (!nomask)
			frame.showGlass();
		DecimalFormat df = new DecimalFormat("0.00");
		api("data", null, result -> {
			frame.content.clear();
			HTMLElement div = InvMon.div("cells");
			frame.content.appendChild(div);

			int maxcells = 0;
			JSONArray abatts = result.getJSONArray("batteries");
			for (int i = 0; i < abatts.length(); ++i) {
				JSONObject jobatt = abatts.getJSONObject(i);
				JSONArray cells = jobatt.getJSONArray("cells");
				maxcells = Math.max(cells.length(), maxcells);
			}

			for (int idx = 0; idx < maxcells; ++idx) {
				HTMLElement div2 = InvMon.div("row");
				for (int j = 0; j < abatts.length(); ++j) {
					float v = abatts.getJSONObject(j).getJSONArray("cells").getJSONObject(idx).getFloat("v");
					div2.appendChild(InvMon.text(df.format(v) + "v"));
				}
				div.appendChild(div2);
			}

			frame.hideOverlays();
		}, err -> {
			frame.showError(err.getMessage());
		});
	}

	@Override
	protected void construct(HTMLElement parent) {
		frame = createStandardFrame(parent, true, "bms-status");
	}

	@Override
	public void configure(JSONObject obj) {
		super.configure(obj);
	}

}
