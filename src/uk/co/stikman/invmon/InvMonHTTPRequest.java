package uk.co.stikman.invmon;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;

public interface InvMonHTTPRequest {

	boolean isMethod(String name);

	String getMethod();

	String getUri();

	Map<String, String> getCookies();

	Map<String, List<String>> getParameters();

	String optParam(String name, String def);
	
	String getQueryParameterString();

}
