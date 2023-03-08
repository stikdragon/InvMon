package uk.co.stikman.invmon.client;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.json.JSONObject;
import org.teavm.classlib.java.net.TURLEncoder;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

public abstract class ClientPage {

	public abstract HTMLElement getElement();

	protected void post(String api, JSONObject args, Consumer<JSONObject> response) {
		post(api, args, response, e -> {
			e.printStackTrace();
			Window.alert("Error from [" + api + "] api: " + e.getMessage());
		});
	}

	protected void post(String api, JSONObject args, Consumer<JSONObject> response, Consumer<Exception> onerror) {
		http("POST", api, args, response, onerror);
	}

	protected void fetch(String api, JSONObject args, Consumer<JSONObject> response) {
		fetch(api, args, response, e -> {
			e.printStackTrace();
			Window.alert("Error from [" + api + "] api: " + e.getMessage());
		});
	}

	protected void fetch(String api, JSONObject args, Consumer<JSONObject> response, Consumer<Exception> onerror) {
		http("GET", api, args, response, onerror);
	}

	private void http(String method, String api, JSONObject args, Consumer<JSONObject> response, Consumer<Exception> onerror) {
		XMLHttpRequest xhr = XMLHttpRequest.create();
		xhr.onComplete(() -> {
			if (xhr.getStatus() != 200)
				onerror.accept(new RPCError(xhr.getStatusText()));
			else
				response.accept(new JSONObject(xhr.getResponseText()));
		});
		if (args != null)
			xhr.open("GET", api + "?" + TURLEncoder.encode(args.toString(), StandardCharsets.UTF_8));
		else
			xhr.open("GET", api);
		xhr.send();
	}

}