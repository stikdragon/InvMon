package uk.co.stikman.invmon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.datamodel.FieldVIF;
import uk.co.stikman.invmon.datamodel.InverterMode;
import uk.co.stikman.invmon.datamodel.VIFReading;
import uk.co.stikman.invmon.inverter.BatteryChargeStage;
import uk.co.stikman.invmon.inverter.InvUtil;
import uk.co.stikman.log.StikLog;

public class TempHTMLOutput extends InvModule {
	private static final StikLog	LOGGER	= StikLog.getLogger(TempHTMLOutput.class);
	private Env						env;
	private String					id;
	private File					target;

	private Field					fieldMode;
	private Field					fieldChargeState;
	private FieldVIF				fieldBattery;
	private FieldVIF				fieldLoad;
	private List<FieldVIF>			fieldPv	= new ArrayList<>();
	private Field					fieldTemperature;
	private Field					fieldBusVoltage;
	private Field					fieldLoadPF;
	private Field					fieldStateOfCharge;
	private Field					fieldMisc;

	public TempHTMLOutput(String id, Env env) {
		super(id, env);
		this.id = id;
		this.env = env;
	}

	@Override
	public void configure(Element config) {
		this.target = new File(InvUtil.getAttrib(config, "target"));
	}

	@Override
	public void start() throws InvMonException {
		super.start();

		DataModel model = getEnv().getModel();
		fieldMode = model.get("INV_MODE");
		fieldChargeState = model.get("BATT_MODE");
		fieldBattery = model.getVIF("BATT");
		fieldLoad = model.getVIF("BATT");

		for (Field f : model)
			if (f.getId().matches("PV[0-9]+_V"))
				fieldPv.add(model.getVIF(f.getId().substring(0, f.getId().length() - 2)));
		fieldTemperature = model.get("INV_TEMP");
		fieldBusVoltage = model.get("INV_BUS_V");
		fieldLoadPF = model.get("LOAD_PF");
		fieldStateOfCharge = model.get("BATT_SOC");
		fieldMisc = model.get("MISC");
	}

	@Override
	public String getId() {
		return id;
	}

	public Env getEnv() {
		return env;
	}

	@Subscribe(Events.POST_DATA)
	public void postData(PollData data) {
		HTMLConsoleThing output = new HTMLConsoleThing();
		output.beginFrame();
		
		
		DataPoint rec = data.get("invA");
		VIFReading battvif = rec.get(fieldBattery);
		output.moveTopLeft();
		output.print("        Battery: ").printFloat(battvif.getV(), 2, 1, "V").print(" (").printFloat(rec.getFloat(fieldStateOfCharge) * 100.0f, 2, 1, "%").print(")").spaces(4).newline();
		String colour = "";
		InverterMode mode = rec.getEnum(fieldMode, InverterMode.class);
		switch (mode) {
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
		String s = mode.name();
		if (mode == InverterMode.CHARGING)
			s += " - " + rec.getEnum(fieldChargeState, BatteryChargeStage.class).name().replaceFirst("CHARGE_", "");
		output.print("Battery current: ").printFloat(Math.abs(battvif.getI()), 2, 1, "A").print("  [ ").color(colour).print(s).reset().print(" ]").spaces(4).newline();
		float pf = rec.getFloat(fieldLoadPF);
		VIFReading loadvif = rec.get(fieldLoad);
		output.print("           Load: ").printInt(loadvif.getP(), 5, "W").print(" (active: ").printInt(loadvif.getP() * pf, 5, "W").print(" PF: ").printFloat(pf, 1, 2).print(")").spaces(4).newline();

		//
		// solar stuff
		//				
		float totp = 0.0f;
		for (FieldVIF f : fieldPv) {
			String name = f.getV().getId().substring(f.getV().getId().length() - 2);
			float p = rec.getFloat(f.getV()) * rec.getFloat(f.getI());
			totp += p;
			output.print("            " + name + ": ").printInt(p, 5, "W").print(" - ").printInt(rec.getFloat(f.getV()), 3, "V").print(" @ ").printFloat(rec.getFloat(f.getI()), 2, 1, "A").spaces(4).newline();
		}
		output.print("       PV Total: ").printInt(totp, 5, "W").spaces(4).newline();

		output.print("    Temperature: ").printFloat(rec.getFloat(fieldTemperature), 2, 1, "C").spaces(4).newline();
		output.print("    Bus Voltage: ").printInt(rec.getFloat(fieldBusVoltage), 3, "V").spaces(4).newline();

		output.print("       Status1 : ").color(ConsoleTextOutput.BRIGHT_PURPLE).print(rec.getString(fieldMisc)).reset().spaces(4).newline();

		
		
		
		output.endFrame();
		
		

		try (FileOutputStream fos = new FileOutputStream(target)) {
			fos.write(output.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			LOGGER.error(e);
		}
	}

}
