package uk.co.stikman.invmon;

public class InvMon {

	public static void main(String[] args) throws Exception {

		Env env = new Env();
		env.start();

		for (;;) {
			int n = System.in.read();
			if (n == 'q') {
				env.terminate();
				break;
			}
		}

		env.awaitTermination();
	}

}
