package uk.co.stikman.invmon.client;

import org.json.JSONObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

public class LoginDialog extends PopupWindow {

	private HTMLInputElement	txtPass;
	private HTMLInputElement	txtUser;
	private ClientPage			owner;
	private HTMLElement			mask;

	public LoginDialog(ClientPage owner) {
		super();
		this.owner = owner;
		getContent().getClassList().add("logindialog");
		HTMLElement h2 = InvMon.text2("h2", "Login");
		getContent().appendChild(h2);

		HTMLElement div = InvMon.div("pair");
		div.appendChild(InvMon.text("Username"));
		getContent().appendChild(div);
		txtUser = InvMon.element("input");
		txtUser.setType("text");
		div.appendChild(txtUser);

		div = InvMon.div("pair");
		div.appendChild(InvMon.text("Password"));
		getContent().appendChild(div);
		txtPass = InvMon.element("input");
		txtPass.setType("password");
		div.appendChild(txtPass);

		div = InvMon.div("bottombar");
		getContent().appendChild(div);
		Button btnLogin = new Button("Login");
		Button btnCancel = new Button("Cancel");
		div.appendChild(btnCancel.getElement());
		div.appendChild(InvMon.div("fill"));
		div.appendChild(btnLogin.getElement());

		btnLogin.setOnClick(this::doLogin);
		btnCancel.setOnClick(this::doCancel);

		mask = InvMon.div("mask");
		mask.appendChild(InvMon.text("Working..."));
		getContent().appendChild(mask);
		mask.getStyle().setProperty("display", "none");
	}

	private void doLogin(Button sender) {
		mask.getStyle().setProperty("display", "flex");
		JSONObject jo = new JSONObject();
		jo.put("user", txtUser.getValue());
		jo.put("pass", txtUser.getValue());
		owner.post("login", jo, res -> {
			if (res.has("error")) {
				Window.alert("Login failed: " + res.getString("error"));
				close();
			}
			LoggedInUser u = new LoggedInUser();
			u.setName(res.getString("name"));
			u.setToken(res.getString("token"));
			InvMon.INSTANCE.setUser(u);
			close();
		});
	}

	private void doCancel(Button sender) {
		mask.getStyle().setProperty("display", "flex");
		JSONObject jo = new JSONObject();
		owner.post("logout", jo, res -> {
			InvMon.INSTANCE.setUser(null);
			close();
		});
	}

}
