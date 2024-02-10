package uk.co.stikman.invmon.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.html.HTMLOptionElement;
import org.teavm.jso.dom.html.HTMLSelectElement;
import org.teavm.jso.dom.html.HTMLTextAreaElement;

import uk.co.stikman.invmon.shared.Option;
import uk.co.stikman.invmon.shared.OptionEnum;
import uk.co.stikman.invmon.shared.OptionFloat;
import uk.co.stikman.invmon.shared.OptionString;
import uk.co.stikman.invmon.shared.OptionStringList;
import uk.co.stikman.invmon.shared.OptionType;
import uk.co.stikman.invmon.shared.WidgetConfigOptions;

public class ConfigPopupWindow extends PopupWindow {

	private HTMLElement																		container;
	private ClientPage																		owner;
	private HTMLElement																		buttons;
	private Consumer<WidgetConfigOptions>													onAccept;
	private WidgetConfigOptions																initialConfig;
	private Map<String, Consumer<Option>>													updaters	= new HashMap<>();

	private static Map<OptionType, BiFunction<ConfigPopupWindow, Option, Consumer<Option>>>	uiFact		= new HashMap<>();

	static {
		uiFact.put(OptionType.STRING, (w, opt) -> w.makeString(opt, 0));
		uiFact.put(OptionType.LONG_STRING, (w, opt) -> w.makeString(opt, 1));
		uiFact.put(OptionType.XML, (w, opt) -> w.makeString(opt, 2));
		uiFact.put(OptionType.FLOAT, ConfigPopupWindow::makeFloat);
		uiFact.put(OptionType.ENUM, ConfigPopupWindow::makeEnum);
		uiFact.put(OptionType.STRING_LIST, ConfigPopupWindow::makeStringList);
	}

	public ConfigPopupWindow(ClientPage owner, WidgetConfigOptions config, Consumer<WidgetConfigOptions> onaccept) {
		super();
		this.owner = owner;
		this.onAccept = onaccept;
		this.initialConfig = config;
		getContent().getClassList().add("config-dialog");

		getContent().appendChild(InvMon.text2("h1", "Configure Widget"));
		container = InvMon.div("list");

		for (Option opt : config) {
			BiFunction<ConfigPopupWindow, Option, Consumer<Option>> f = uiFact.get(opt.getType());
			if (f != null) {
				updaters.put(opt.getName(), f.apply(this, opt));
			} else
				System.err.println("WARNING: No factory for " + opt.getType() + " option");
		}
		getContent().appendChild(container);

		buttons = InvMon.div("buttons");
		buttons.appendChild(new Button("OK", this::accept).getElement());
		buttons.appendChild(InvMon.div("fill"));
		buttons.appendChild(new Button("Cancel", e -> close()).getElement());

		getContent().appendChild(buttons);
	}

	private void accept(Button b) {
		WidgetConfigOptions wco = new WidgetConfigOptions();
		//
		// make a deep copy, being lazy and just usign the existing
		// serialiser stuff for this
		//
		JSONObject jo = new JSONObject();
		initialConfig.toJSON(jo);
		wco.fromJSON(jo);

		for (Option o : wco)
			updaters.get(o.getName()).accept(o);

		if (onAccept != null)
			onAccept.accept(wco);
		close();
	}

	private Consumer<Option> makeStringList(Option opt_) {
		OptionStringList opt = (OptionStringList) opt_;
		HTMLTextAreaElement el = InvMon.element("textarea");
		el.setTextContent(opt.getValue().stream().collect(Collectors.joining("\n")));
		addRow(opt.getDisplay(), el);
		return x -> {
			OptionStringList osl = (OptionStringList) x;
			osl.getValue().clear();
			for (String s : el.getValue().split("\n"))
				osl.getValue().add(s);
		};
	}

	private Consumer<Option> makeString(Option opt_, int mode) {
		OptionString opt = (OptionString) opt_;
		if (mode == 0) {
			HTMLInputElement el = InvMon.element("input");
			el.setValue(opt.getValue());
			addRow(opt.getDisplay(), el);
			return x -> ((OptionString) x).setValue(el.getValue());
		} else {
			HTMLTextAreaElement el = InvMon.element("textarea");
			el.setTextContent(opt.getValue());
			addRow(opt.getDisplay(), el);
			return x -> ((OptionString) x).setValue(el.getValue());
		}
	}

	private Consumer<Option> makeEnum(Option opt_) {
		OptionEnum opt = (OptionEnum) opt_;
		HTMLSelectElement cbo = InvMon.element("select");
		for (String s : opt.getValues()) {
			HTMLOptionElement el = InvMon.element("option");
			el.setValue(s);
			el.setTextContent(s);
			cbo.appendChild(el);
		}
		cbo.setSelectedIndex(opt.getValues().indexOf(opt.getValue()));
		addRow(opt_.getDisplay(), cbo);
		return x -> ((OptionEnum) x).setValue(opt.getValues().get(cbo.getSelectedIndex()));
	}

	private Consumer<Option> makeFloat(Option opt_) {
		OptionFloat opt = (OptionFloat) opt_;
		HTMLInputElement el = InvMon.element("input");
		el.setValue(Float.toString(opt.getValue()));
		addRow(opt.getDisplay(), el);
		return x -> ((OptionFloat) x).setValue(Float.parseFloat(el.getValue()));
	}

	private void addRow(String display, HTMLElement el) {
		HTMLElement div = InvMon.div("row");
		div.appendChild(InvMon.text(display + ": ", "left"));
		div.appendChild(el);
		container.appendChild(div);
	}
}
