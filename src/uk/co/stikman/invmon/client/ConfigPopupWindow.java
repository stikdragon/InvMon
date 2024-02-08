package uk.co.stikman.invmon.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

import uk.co.stikman.invmon.shared.Option;
import uk.co.stikman.invmon.shared.OptionFloat;
import uk.co.stikman.invmon.shared.OptionString;
import uk.co.stikman.invmon.shared.OptionType;
import uk.co.stikman.invmon.shared.WidgetConfigOptions;

public class ConfigPopupWindow extends PopupWindow {

	private HTMLElement														container;
	private ClientPage														owner;

	private static Map<OptionType, BiConsumer<ConfigPopupWindow, Option>>	uiFact	= new HashMap<>();

	static {
		uiFact.put(OptionType.STRING, ConfigPopupWindow::makeString);
		uiFact.put(OptionType.FLOAT, ConfigPopupWindow::makeFloat);
		uiFact.put(OptionType.ENUM, ConfigPopupWindow::makeEnum);
	}

	public ConfigPopupWindow(ClientPage owner, WidgetConfigOptions config, Consumer<WidgetConfigOptions> onaccept) {
		super();
		this.owner = owner;

		getContent().getClassList().add("config-dialog");

		container = InvMon.div("list");

		for (Option opt : config) {
			BiConsumer<ConfigPopupWindow, Option> f = uiFact.get(opt.getType());
			if (f != null)
				f.accept(this, opt);
			else
				System.err.println("WARNING: No factory for " + opt.getType() + " option");
		}
		getContent().appendChild(container);
	}

	private void makeString(Option opt_) {
		OptionString opt = (OptionString) opt_;
		HTMLInputElement el = InvMon.element("input");
		el.setValue(opt.getValue());
		addRow(opt.getDisplay(), el);
	}

	private void makeEnum(Option opt_) {
		addRow(opt_.getDisplay(), InvMon.text("ex"));
	}
	private void makeFloat(Option opt_) {
		OptionFloat opt = (OptionFloat) opt_;
		HTMLInputElement el = InvMon.element("input");
		el.setValue(Float.toString(opt.getValue()));
		addRow(opt.getDisplay(), el);
	}

	private void addRow(String display, HTMLElement el) {
		HTMLElement div = InvMon.div("row");
		div.appendChild(InvMon.text(display));
		div.appendChild(el);
		container.appendChild(div);
	}
}
