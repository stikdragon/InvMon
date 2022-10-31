package uk.co.stikman.invmon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.inverter.InverterUtils;
import uk.co.stikman.log.StikLog;

public class HTMLOutput implements ProcessPart, RecordListener {
	private static final StikLog	LOGGER	= StikLog.getLogger(HTMLOutput.class);
	private Env						env;
	private String					id;
	private File					target;

	public HTMLOutput(String id, Env env) {
		this.id = id;
		this.env = env;
	}

	@Override
	public void configure(Element config) {
		this.target = new File(InverterUtils.getAttrib(config, "target"));
	}

	@Override
	public void start() {
		env.addListener(this);
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
		HTMLConsoleThing console = new HTMLConsoleThing();
		console.beginFrame();
		console.print("        Battery: ").printFloat(rec.getBattery().getV(), 2, 1, "V").print(" (").printFloat(rec.getStateOfCharge() * 100.0f, 2, 1, "%").print(")").newline();
		String colour = "";
		switch (rec.getMode()) {
		case CHARGING:
			colour = HTMLConsoleThing.BRIGHT_GREEN;
			break;
		case DISCHARGING:
		case ERROR:
			colour = HTMLConsoleThing.BRIGHT_RED;
			break;
		case OFFLINE:
			colour = HTMLConsoleThing.BRIGHT_BLACK;
			break;
		}
		console.print("Battery current: ").printFloat(Math.abs(rec.getBattery().getI()), 2, 1, "A").print("  [ ").color(colour).print(rec.getMode().name()).reset().print(" ]").newline();
		float pf = rec.getLoadPF();
		console.print("           Load: ").printInt(rec.getLoad().getP(), 5, "W").print(" (active: ").printInt(rec.getLoad().getP() * pf, 5, "W").print(" PF: ").printFloat(pf, 1, 2).print(")").newline();
		//
		// solar stuff
		//
		//				console.print("           Mode: [ ").color(ConsoleOutput.BRIGHT_YELLOW).print(inv.getDeviceMode().name()).reset().print(" ]").spaces(4).newline();
		float totp = 0.0f;
		for (int i = 0; i < rec.getPvCount(); ++i) {
			console.print("            PV" + (i + 1) + ": ").printInt(rec.getPv(i).getP(), 5, "W").print(" - ").printInt(rec.getPv(i).getV(), 3, "V").print(" @ ").printFloat(rec.getPv(i).getI(), 2, 1, "A").newline();
			totp += rec.getPv(i).getP();
		}

		console.print("       PV Total: ").printInt(totp, 5, "W").spaces(4).newline();

		console.print("    Temperature: ").printFloat(rec.getTemperature(), 2, 1, "C").spaces(4).newline();
		console.print("    Bus Voltage: ").printInt(rec.getBusVoltage(), 3, "V").spaces(4).newline();

		console.print("Status1 :").color(ConsoleTextOutput.BRIGHT_PURPLE).print(rec.getMisc()).reset().spaces(4).newline();

		console.endFrame();

		try (FileOutputStream fos = new FileOutputStream(target)) {
			fos.write(console.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			LOGGER.error(e);
		}
	}

	@Override
	public void terminate() {
	}

}
