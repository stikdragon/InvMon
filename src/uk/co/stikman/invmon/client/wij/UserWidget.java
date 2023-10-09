package uk.co.stikman.invmon.client.wij;

import org.teavm.jso.dom.html.HTMLAnchorElement;
import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.client.ClientPage;
import uk.co.stikman.invmon.client.InvMon;
import uk.co.stikman.invmon.client.LoggedInUser;

public class UserWidget {

	private HTMLElement	root;
	private ClientPage	owner;

	public UserWidget(ClientPage owner) {
		this.owner = owner;
		root = InvMon.div("accountwidget");
		owner.getBus().subscribe(Events.USER_LOGGED_IN, this::loggedIn);
		owner.getBus().subscribe(Events.USER_LOGGED_OUT, this::loggedOut);
		update();
	}

	private void update() {
		LoggedInUser user = InvMon.INSTANCE.getUser();
		root.clear();
		if (user == null) {
			HTMLAnchorElement a = InvMon.element("a");
			a.setHref("#");
			a.setTextContent("Login..");
			a.addEventListener("click", ev -> {
				owner.doLogin();
			});
			root.appendChild(a);
		} else {
			HTMLElement div = InvMon.div();
			div.setTextContent("User: " + user.getName());
			root.appendChild(div);

			HTMLAnchorElement a = InvMon.element("a");
			a.setTextContent("(Logout)");
			a.setHref("#");
			a.addEventListener("click", ev -> {
				owner.doLogout();
			});
			root.appendChild(a);
		}
	}

	public HTMLElement getElement() {
		return root;
	}

	private void loggedIn(Object usr) {
		update();
	}

	private void loggedOut(Object v) {
		update();
	}

}
