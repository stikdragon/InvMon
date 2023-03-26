package uk.co.stikman.invmon;

public class InvMon {

	public static void main(String[] args) throws Exception {

//		new File("C:\\Stik\\java\\InvMon\\data\\datalog.db").delete();
//		new File("C:\\Stik\\java\\InvMon\\data\\datalog.db.0").delete();
//		new File("C:\\Stik\\java\\InvMon\\data\\datalog.db.1").delete();
//		new File("C:\\Stik\\java\\InvMon\\data\\datalog.db.2").delete();
//
//		Files.copy(Paths.get("C:\\Stik\\java\\InvMon\\data\\frompi\\red\\datalog.db"), Paths.get("C:\\Stik\\java\\InvMon\\data\\datalog.db"));
//		Files.copy(Paths.get("C:\\Stik\\java\\InvMon\\data\\frompi\\red\\datalog.db.0"), Paths.get("C:\\Stik\\java\\InvMon\\data\\datalog.db.0"));
//		Files.copy(Paths.get("C:\\Stik\\java\\InvMon\\data\\frompi\\red\\datalog.db.1"), Paths.get("C:\\Stik\\java\\InvMon\\data\\datalog.db.1"));
//		Files.copy(Paths.get("C:\\Stik\\java\\InvMon\\data\\frompi\\red\\datalog.db.2"), Paths.get("C:\\Stik\\java\\InvMon\\data\\datalog.db.2"));
		
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
