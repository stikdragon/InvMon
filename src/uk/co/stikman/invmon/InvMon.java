package uk.co.stikman.invmon;

public class InvMon {

	public static void main(String[] args) throws Exception {

		Env env = new Env();
		env.start();

		for (;;) {
			int n = System.in.read();
			if (n == 'q') {
				try {
					env.terminate();
					System.exit(0);
				} catch (Throwable th) {
					System.err.println("Shutdown failed: " + th.getMessage());
					System.exit(-1);
				}
				break;
			}
		}
	}

}
