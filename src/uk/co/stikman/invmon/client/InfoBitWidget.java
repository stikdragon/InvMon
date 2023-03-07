package uk.co.stikman.invmon.client;

import org.teavm.jso.dom.html.HTMLElement;

public class InfoBitWidget extends AbstractPageWidget {

	private HTMLElement root;

	public InfoBitWidget(ClientPage owner) {
		super(owner);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void construct(HTMLElement parent) {
		this.root = InvMon.div();
		parent.appendChild(root);
		doLayout(root);
	}

	@Override
	protected void refresh() {
		getOwner().fetch("getInfoBit", null, result -> root.setInnerHTML(result.getString("html")));
	}

}
