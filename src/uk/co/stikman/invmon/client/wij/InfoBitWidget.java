package uk.co.stikman.invmon.client.wij;

import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.client.AbstractPageWidget;
import uk.co.stikman.invmon.client.ClientPage;
import uk.co.stikman.invmon.client.InvMon;

public class InfoBitWidget extends AbstractPageWidget {

	private HTMLElement root;

	public InfoBitWidget(ClientPage owner) {
		super(owner);
	}

	@Override
	protected void construct(HTMLElement parent) {
		this.root = InvMon.div();
		parent.appendChild(root);
		doLayout(root);
	}

	@Override
	protected void refresh(boolean nomask) {
		api("get", null, result -> root.setInnerHTML(result.getString("html")));
	}

}
