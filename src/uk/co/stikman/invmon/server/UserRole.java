package uk.co.stikman.invmon.server;

public enum UserRole {
	DISABLED(0), 
	PUBLIC(1), 
	USER(2), 
	ADMIN(3);

	private final int level;

	UserRole(int lvl) {
		this.level = lvl;
	}

	public int getLevel() {
		return level;
	}
}
