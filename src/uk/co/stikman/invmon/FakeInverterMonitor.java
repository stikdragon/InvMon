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
		return super.consoleCommand(console, cmd);
	}
	
	

}
