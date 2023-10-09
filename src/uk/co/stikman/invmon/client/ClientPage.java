package uk.co.stikman.invmon.client;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.json.JSONObject;
import org.teavm.classlib.java.net.TURLEncoder;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.Events;

/**
 * a page is a collection of Widget objects. Most widgets have a corresponding
 * instance on the server, which is linked by its ID. a widget can make a call
 * to its back end object
 * 
 * @author stik
 *
 */
public abstract class ClientPage {
	private ReallySimpleEventBus bus = new ReallySimpleEventBus();

	public abstract HTMLElement getElement();

	protected void post(String api, JSONObject args, Consumer<JSONObject> response) {
		post(api, args, response, e -> {
			ClientUtil.handleError(new RPCError(e));
		});
	}

	protected void post(String api, JSONObject args, Consumer<JSONObject> response, Consumer<String> onerror) {
		http("POST", api, args, response, onerror);
	}

	protected void fetch(String api, JSONObject args, Consumer<JSONObject> response) {
		fetch(api, args, response, e -> {
			ClientUtil.handleError(new RPCError(e));
		});
	}

	public void fetch(String api, JSONObject args, Consumer<JSONObject> response, Consumer<String> onerror) {
		http("GET", api, args, response, onerror);
	}

	private void http(String method, String api, JSONObject args, Consumer<JSONObject> response, Consumer<String> onerror) {
		XMLHttpRequest xhr = XMLHttpRequest.create();
		xhr.onComplete(() -> {
			if (xhr.getStatus() != 200) {
				String err = method.equals("POST") ? xhr.getResponseText() : xhr.getStatusText();
				onerror.accept(err);
			} else {
				response.accept(new JSONObject(xhr.getResponseText()));
			}
		});

		if (method.equals("POST")) {
			xhr.open("POST", api);
			if (args != null) {
				xhr.setRequestHeader("Content-type", "application/json");
				xhr.send(args.toString());
			} else {
				xhr.send();
			}

		} else {
			if (args != null)
				xhr.open("GET", api + "?" + TURLEncoder.encode(args.toString(), StandardCharsets.UTF_8));
			else
				xhr.open("GET", api);
			xhr.send();
		}
	}

	public ReallySimpleEventBus getBus() {
		return bus;
	}

	public void doLogin() {
		LoginDialog dlg = new LoginDialog(this);
		dlg.showModal();
	}

	public void doLogout() {
		post("logout", new JSONObject(), res -> {
		});
		InvMon.INSTANCE.setUser(null);
		bus.fire(Events.USER_LOGGED_OUT, null);
	}

}
