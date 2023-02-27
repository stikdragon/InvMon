package uk.co.stikman.invmon.client;

import org.teavm.jso.dom.html.HTMLElement;

public class TimeSelector extends PageWidget {

	private HTMLElement controlsEl;

	public TimeSelector(ClientPage owner) {
		super(owner);
	}

	@Override
	protected void refresh() {
	}

	@Override
	protected void construct(HTMLElement parent) {
		StandardFrame frm = createStandardFrame(parent, false, "timeselector");

		this.controlsEl = InvMon.div("controls");
		frm.content.appendChild(controlsEl);
		addRange(5, "5 Min");
		addRange(30, "30 Min");
		addRange(60, "1 Hour");
		addRange(120, "2 Hour");
		addRange(360, "6 Hour");
		addRange(720, "12 Hour");
		addRange(1440, "24 Hour");
		addRange(2880, "2 Day");
		addRange(7200, "5 Day");
		addRange(43200, "30 Day");
	}

	private void addRange(int n, String display) {
		HTMLElement el = InvMon.element("a", "unsel");
		el.setInnerText(display);
		controlsEl.appendChild(el);
		controlsEl.appendChild(InvMon.div("divider"));
	}

}
