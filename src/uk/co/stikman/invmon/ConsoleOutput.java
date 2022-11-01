package uk.co.stikman.invmon;

import org.w3c.dom.Element;

public class ConsoleOutput implements ProcessPart, RecordListener {

	private Env					env;
	private String				id;
	private ConsoleTextOutput	console;
	private boolean				firstTime	= true;

	public ConsoleOutput(String id, Env env) {
		this.id = id;
		this.env = env;
	}

	@Override
	public void configure(Element config) {
	}

	@Override
	public void start() {
		console = new ConsoleTextOutput(System.out);
		env.addListener(this);
		console.clear();
	}

	@Override
	public String getId() {
		return id;
	}

	public Env getEnv() {
		return env;
	}

	@Override
	public void record(long id, InverterDataPoint rec) {
		if (firstTime) {
			console.clear();
			firstTime = false;
		}
		console.beginFrame();
		console.hideCursor();

		console.moveTopLeft();
		console.print("        Battery: ").printFloat(rec.getBattery().getV(), 2, 1, "V").print(" (").printFloat(rec.getStateOfCharge() * 100.0f, 2, 1, "%").print(")").spaces(4).newline();
		String colour = "";
		switch (rec.getMode()) {
			case CHARGING:
				colour = ConsoleTextOutput.BRIGHT_GREEN;
				break;
			case DISCHARGING:
			case ERROR:
				colour = ConsoleTextOutput.BRIGHT_RED;
				break;
			case OFFLINE:
				colour = ConsoleTextOutput.BRIGHT_BLACK;
				break;
		}
		String s = rec.getMode().name();
		if (rec.getMode() == InverterMode.CHARGING)
			s += " - " + rec.getChargeState().name().replaceFirst("CHARGE_", "");
		console.print("Battery current: ").printFloat(Math.abs(rec.getBattery().getI()), 2, 1, "A").print("  [ ").color(colour).print(s).reset().print(" ]").spaces(4).newline();
		float pf = rec.getLoadPF();
		console.print("           Load: ").printInt(rec.getLoad().getP(), 5, "W").print(" (active: ").printInt(rec.getLoad().getP() * pf, 5, "W").print(" PF: ").printFloat(pf, 1, 2).print(")").spaces(4).newline();

		//
		// solar stuff
		//				
		float totp = 0.0f;
		for (int i = 0; i < rec.getPvCount(); ++i) {
			console.print("            PV" + (i + 1) + ": ").printInt(rec.getPv(i).getP(), 5, "W").print(" - ").printInt(rec.getPv(i).getV(), 3, "V").print(" @ ").printFloat(rec.getPv(i).getI(), 2, 1, "A").spaces(4).newline();
			totp += rec.getPv(i).getP();
		}

		console.print("       PV Total: ").printInt(totp, 5, "W").spaces(4).newline();

		console.print("    Temperature: ").printFloat(rec.getTemperature(), 2, 1, "C").spaces(4).newline();
		console.print("    Bus Voltage: ").printInt(rec.getBusVoltage(), 3, "V").spaces(4).newline();

		console.print("       Status1 : ").color(ConsoleTextOutput.BRIGHT_PURPLE).print(rec.getMisc()).reset().spaces(4).newline();

		console.showCursor();
		console.endFrame();

	}

	@Override
	public void terminate() {
		console = null;
	}

}
