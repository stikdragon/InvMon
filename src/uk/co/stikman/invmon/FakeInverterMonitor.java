package uk.co.stikman.invmon;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.datamodel.InverterMode;
import uk.co.stikman.invmon.inverter.BatteryChargeStage;
import uk.co.stikman.invmon.inverter.PIP8048MAX.OutputMode;
import uk.co.stikman.invmon.server.Console;
import uk.co.stikman.log.StikLog;

@ModType(InvModType.INVERTER)
public class FakeInverterMonitor extends InverterMonitor {
	private static final StikLog LOGGER = StikLog.getLogger(FakeInverterMonitor.class);

	public FakeInverterMonitor(String id, Env env) {
		super(id, env);
	}

	@Override
	public void configure(Element config) {
	}

	@Override
	public void start() throws InvMonException {
		super.start();
	}

	@Override
	public Sample createDataPoint(long ts) {
		Sample dp = new Sample(ts);
		dp.put("mode", InverterMode.CHARGING);
		dp.put("chargeState", BatteryChargeStage.CHARGE_FLOAT);
		dp.put("battI", rand(90f));
		dp.put("battV", 50f + rand(20f));
		dp.put("gridI", rand(90f));
		dp.put("gridV", rand(240f));
		dp.put("gridF", 50.0f);
		dp.put("loadI", 1.45f + rand(3f));
		dp.put("loadV", 230.0f + rand(10f));
		dp.put("loadF", 50.0f);
		dp.put("pv1", 304f + rand(40f), 4.0f + rand(7f), 0);
		dp.put("pv2", 304f + rand(40f), 4.0f + rand(7f), 0);
		dp.put("temp", 41f + rand(5f));
		dp.put("busV", (int) (380 + rand(100)));
		dp.put("loadPF", rand(1.0f));
		dp.put("soc", 0.52f + rand(0.5f));
		
		
		File f = new File("C:\\junk\\pv.txt");
		if (f.exists()) {
			try {
				String s = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
				JSONObject jo = new JSONObject(s);
				dp.put("pv1", jo.getFloat("pv1v"), jo.getFloat("pv1i"), 0);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		
		return dp;
	}

	private float rand(float f) {
		return (float) ((0.5f - Math.random()) * f);
	}

	@Override
	public void terminate() {
		super.terminate();
	}

	@Override
	public void setOutputMode(OutputMode mode) {
		LOGGER.info("Output mode set to: " + mode);
	}
	
	@Override
	protected void populateCommandHelp(List<ConsoleHelpInfo> lst) {
		super.populateCommandHelp(lst);
		lst.add(new ConsoleHelpInfo("qpigs", "show the last QPIGS response received (FAKE)"));
	}

	@Override
	public ConsoleResponse consoleCommand(Console console, String cmd) throws InvMonException {
		if (cmd.equals("qpigs")) {
			return new ConsoleResponse("QPIGS RESPONSE 1234.0 123 125.5 125 6236 01001100 etc...");
		}
		if (cmd.equals("test")) {
			return new ConsoleResponse("""
					^3CALIBRATE CELL VOLTAGES^x:
					========================
					    ^5calib cell reset
					    calib cell all [voltage]
					    calib cell ^2[ID]^5=^2[voltage]^x

					Write a calibration factor back to the BMS.  You tell the BMS what the voltage
					on a particular channel really is by measuring it accurately.  This allows it to
					calibrate its ADCs.   The easiest way to do this is to connect all the channels
					to the highest cell (eg. the ^5+ve^x terminal of the battery pack) at once, and then
					tell use the "^5all^x" method.  this way you only have to make one measurement.

					It's important the battery is in a very steady state when you do this.  It's best
					to leave the battery disconnected from any load so that you can depend on the
					voltage you measured not changing in the time it takes you to issue the command.

					Eg. if you measure the total pack voltage as ^553.45v^x then issue the command:

					    ^5calib cell all 53.45^x

					You can do this cell-by-cell as well, if you want.  Cells start at index ^51^x.

					    ^5calib cell 4=13.34, 5=16.66, 6=20.02^x

					Also

					    ^5calib offset -0.05
					    calib raw all 13.5^x        ^4  // set the factor directly (advanced command)^x
					    ^5calib raw 0=13.5, 1=14.1, 2=13.9

					The "^5reset^x" option sets all the factors back to ^51.0^x, which is "uncalibrated"

					^3CALIBRATE CURRENT SHUNT^x:
					========================
					    ^5calib shunt reset
					    calib shunt ^2[current]^x

					Similar to above, this calibrates the current shunt ADC.  This is more difficult
					to achieve as you need a steady current flowing.  The easiest way to do this is
					probably to have the system running a constant load, like a space heater that's
					been left to settle for a couple of minutes.   You need an accurate way of
					measuring the current flowing from the battery.  For example, if you measure
					^585.2A^x flowing then issue the command:

					    ^5calib shunt 85.2^x

					You can also do:

					    ^5calib show^x

                    which lists all the current calibration constants.  This isn't very useful.

					NOTE: changing calibration factors stores them in EEPROM memory in the BMS.  this
					has a limited number of write-cycles, so don't do this many tens of thousands of times
					""", true);
			
		}
		return super.consoleCommand(console, cmd);
	}
	
	

}
