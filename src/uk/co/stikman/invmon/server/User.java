package uk.co.stikman.invmon.server;

public class User {
	private String		name;
	private String		pass;
	private UserRole	role;

	public User(String name, String pass, UserRole role) {
		super();
		this.name = name;
		this.pass = pass;
		this.role = role;
	}

	public String getName() {
		return name;
	}

	public String getPass() {
		return pass;
	}

	public UserRole getRole() {
		return role;
	}

	@Override
	public String toString() {
		return "User [name=" + name + ", role=" + role + "]";
	}

}
