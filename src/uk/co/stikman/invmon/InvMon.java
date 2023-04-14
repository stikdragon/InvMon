package uk.co.stikman.invmon;

import java.io.File;

public class InvMon {

	private static Env svcmode;

	public static void main(String[] args) throws Exception {
		//
		// called if executed as an ordinary java process
		//
		Env env = new Env();
		env.start(new File(System.getProperty("user.dir")));

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

	//
	// start and stop are called by procrun, when setting this up
	// as a windows service
	//
	public static void start(String[] args) {
		svcmode = new Env();
		try {
			svcmode.start(new File(args[0]));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void stop(String[] args) {
		try {
			if (svcmode != null)
				svcmode.terminate();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
