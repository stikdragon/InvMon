package uk.co.stikman.invmon.client;

public class LoggedInUser {
	private String	name;
	private String	token;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
