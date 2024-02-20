package uk.co.stikman.invmon;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.datamodel.FieldVIF;
import uk.co.stikman.invmon.datamodel.InverterMode;
import uk.co.stikman.invmon.datamodel.ModelField;
import uk.co.stikman.invmon.datamodel.VIFReading;
import uk.co.stikman.invmon.inverter.BatteryChargeStage;
import uk.co.stikman.invmon.inverter.util.InvUtil;

public class ConsoleOutput extends InvModule {

	private ConsoleTextOutput	output;
	private boolean				firstTime	= true;

	private ModelField				fieldMode;
	private ModelField				fieldChargeState;
	private FieldVIF			fieldLoad;
	private List<FieldVIF>		fieldPv		= new ArrayList<>();
	private ModelField				fieldBusVoltage;
	private ModelField				fieldLoadPF;
	private ModelField				fieldStateOfCharge;
	private ModelField				fieldMisc;
	private boolean				enabledControlCodes;
	private ModelField				fieldTemperature1;
	private ModelField				fieldTemperature2;
	private ModelField				fieldBatteryV;
	private ModelField				fieldBatteryI;

	public ConsoleOutput(String id, Env env) {
		super(id, env);
	}

	@Override
	public void configure(Element config) {
		enabledControlCodes = Boolean.parseBoolean(InvUtil.getAttrib(config, "controlCodes", "true"));
	}

	@Override
	public void start() throws InvMonException {
		super.start();

		DataModel model = getEnv().getModel();
		fieldMode = model.get("INV_MODE");
		fieldChargeState = model.get("BATT_MODE");
		fieldBatteryV = model.get("BATT_V");
		fieldBatteryI = model.get("BATT_I");
		fieldLoad = model.getVIF("LOAD");

		for (ModelField f : model)
			if (f.getId().matches("PV[0-9]+_V"))
				fieldPv.add(model.getVIF(f.getId().substring(0, f.getId().length() - 2)));
		fieldTemperature1 = model.get("INV_1_TEMP");
		fieldTemperature2 = model.get("INV_2_TEMP");
		fieldBusVoltage = model.get("INV_1_BUS_V");
		fieldLoadPF = model.get("LOAD_PF");
		fieldStateOfCharge = model.get("BATT_SOC");
		fieldMisc = model.get("MISC");

		output = new ConsoleTextOutput(System.out);
		output.setEnableControlCodes(enabledControlCodes);
	}

	@Override
	public void terminate() {
		output = null;
		super.terminate();
	}

	@Subscribe(Events.LOGGER_RECORD_COMMITED)
	public void postData(DBRecord rec) {
		synchronized (output) {
			if (firstTime) {
				output.clear();
				firstTime = false;
			}
			output.beginFrame();
			output.hideCursor();

			float battv = rec.getFloat(fieldBatteryV);
			float batti = rec.getFloat(fieldBatteryI);
			output.moveTopLeft();
			output.print("        Battery: ").printFloat(battv, 2, 1, "V").print(" (").printFloat(rec.getFloat(fieldStateOfCharge) * 100.0f, 2, 1, "%").print(")").spaces(4).newline();
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
			output.print("Battery current: ").printFloat(Math.abs(batti), 2, 1, "A").print("  [ ").color(colour).print(s).reset().print(" ]").spaces(4).newline();
			float pf = rec.getFloat(fieldLoadPF);
			VIFReading loadvif = rec.getVIF(fieldLoad);
			output.print("           Load: ").printInt(loadvif.getP(), 5, "W").print(" (active: ").printInt(loadvif.getP() * pf, 5, "W").print(" PF: ").printFloat(pf, 1, 2).print(")").spaces(4).newline();

			//
			// solar stuff
			//				
			float totp = 0.0f;
			for (FieldVIF f : fieldPv) {
				String name = f.getV().getId().substring(0, f.getV().getId().length() - 2);
				float p = rec.getFloat(f.getV()) * rec.getFloat(f.getI());
				totp += p;
				output.print("            " + name + ": ").printInt(p, 5, "W").print(" - ").printInt(rec.getFloat(f.getV()), 3, "V").print(" @ ").printFloat(rec.getFloat(f.getI()), 2, 1, "A").spaces(4).newline();
			}
			output.print("       PV Total: ").printInt(totp, 5, "W").spaces(4).newline();

			output.print("    Temperature: ").printFloat(Math.max(rec.getFloat(fieldTemperature1), rec.getFloat(fieldTemperature2)), 2, 1, "C").spaces(4).newline();
			output.print("    Bus Voltage: ").printInt(rec.getFloat(fieldBusVoltage), 3, "V").spaces(4).newline();

			output.print("       Status1 : ").color(ConsoleTextOutput.BRIGHT_PURPLE).print(rec.getString(fieldMisc)).reset().spaces(4).newline();

			output.showCursor();
			output.endFrame();
		}
	}

}
