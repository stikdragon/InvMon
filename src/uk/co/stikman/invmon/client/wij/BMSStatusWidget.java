package uk.co.stikman.invmon.client.wij;

import java.text.DecimalFormat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Element;

import uk.co.stikman.invmon.client.AbstractPageWidget;
import uk.co.stikman.invmon.client.ClientPage;
import uk.co.stikman.invmon.client.InvMon;
import uk.co.stikman.invmon.client.StandardFrame;
import uk.co.stikman.invmon.client.ToggleButton;

public class BMSStatusWidget extends AbstractPageWidget {
	private static final DecimalFormat df = new DecimalFormat("0.00");

	public class CellThing {

		private Element		root;
		private float		dv;
		private float		v;
		private HTMLElement	barouter;
		private HTMLElement	txt;

		public CellThing(float v, float dv) {
			this.v = v;
			this.dv = dv;
			//2.50V-3.65V
			int pct = (int) (100.0f * (v - 2.50f) / (3.65f - 2.50f));
			if (pct < 0)
				pct = 0;
			if (pct > 100)
				pct = 100;

			root = InvMon.div("cell");
			barouter = InvMon.div("barouter");
			if (showDelta)
				txt = InvMon.text((int) (dv * 1000) + " mV", "txt");
			else
				txt = InvMon.text(df.format(v) + " V", "txt");
			root.appendChild(barouter);
			root.appendChild(txt);
			barouter.getStyle().setProperty("width", pct + "%");
			HTMLElement barinner = InvMon.div("barinner");
			barouter.appendChild(barinner);
		}

		public Element getElement() {
			return root;
		}

	}

	private StandardFrame	frame;
	private HTMLElement		cellsdiv;
	private boolean			showDelta	= false;
	private JSONObject		lastResults;

	public BMSStatusWidget(ClientPage owner) {
		super(owner);
	}

	@Override
	protected void refresh(boolean nomask) {
		if (!nomask)
			frame.showGlass();

		api("data", null, result -> {
			lastResults = result;
			redraw();
		}, err -> {
			frame.showError(err.getMessage());
		});
	}

	@Override
	protected void construct(HTMLElement parent) {
		frame = createStandardFrame(parent, true, "bms-status");

		this.cellsdiv = InvMon.div("cells");
		HTMLElement top = InvMon.div("top");
		ToggleButton btnDV = new ToggleButton("dV");
		btnDV.setOnClick(b -> {
			showDelta = b.getState();
			redraw();
		});

		frame.content.clear();
		frame.content.appendChild(btnDV.getElement());
		frame.content.appendChild(cellsdiv);
	}

	private void redraw() {
		cellsdiv.clear();
		if (lastResults == null)
			return;

		int maxcells = 0;
		float avg = 0.0f;
		JSONArray abatts = lastResults.getJSONArray("batteries");
		for (int i = 0; i < abatts.length(); ++i) {
			JSONObject jobatt = abatts.getJSONObject(i);
			JSONArray cells = jobatt.getJSONArray("cells");
			maxcells = Math.max(cells.length(), maxcells);
			avg += cells.getJSONObject(i).getFloat("v");
		}
		avg /= maxcells;

		for (int idx = 0; idx < maxcells; ++idx) {
			HTMLElement div2 = InvMon.div("row");
			for (int j = 0; j < abatts.length(); ++j) {
				JSONArray cells = abatts.getJSONObject(j).getJSONArray("cells");
				if (cells.length() > idx) {
					float v = cells.getJSONObject(idx).getFloat("v");
					float dv = v - avg;
					div2.appendChild(new CellThing(v, dv).getElement());
					//						div2.appendChild(InvMon.text(df.format(v) + "v"));
				}
			}
			cellsdiv.appendChild(div2);
		}

		frame.hideOverlays();
	}

	@Override
	public void configure(JSONObject obj) {
		super.configure(obj);
	}

}
