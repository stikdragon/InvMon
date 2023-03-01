package uk.co.stikman.invmon.client;

import java.util.ArrayList;
import java.util.List;

import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.client.TimeSelector.UpdateDataOptionsHandler;
import uk.co.stikman.invmon.inverter.InvUtil;

public class TimeSelector extends PageWidget {

	public interface UpdateDataOptionsHandler {
		void updateDataOptions(int offset, int duration);
	}

	private HTMLElement					controlsEl;
	private List<HTMLElement>			items	= new ArrayList<>();
	private UpdateDataOptionsHandler	onChange;

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
		el.setAttribute("data-len", Integer.toString(n));
		controlsEl.appendChild(InvMon.div("divider"));
		el.listenClick(ev -> select(el));
		items.add(el);
	}

	private void select(HTMLElement el) {
		for (HTMLElement x : items)
			x.setAttribute("class", x == el ? "sel" : "unsel");
		int n = Integer.parseInt(el.getAttribute("data-len"));
		if (onChange != null)
			onChange.updateDataOptions(-1, n);
	}

	public UpdateDataOptionsHandler getOnChange() {
		return onChange;
	}

	public void setOnChange(UpdateDataOptionsHandler onChange) {
		this.onChange = onChange;
	}

	public void setCurrent(int n) {
		for (HTMLElement el : items)
			if (Integer.parseInt(el.getAttribute("data-len")) == n)
				select(el);
	}

}
