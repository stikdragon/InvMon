package uk.co.stikman.invmon.inverter.PIP8048MAX;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import org.w3c.dom.Element;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.stikman.invmon.ConsoleHelpInfo;
import uk.co.stikman.invmon.ConsoleResponse;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.InvModType;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.InverterMonitor;
import uk.co.stikman.invmon.ModType;
import uk.co.stikman.invmon.Sample;
import uk.co.stikman.invmon.datamodel.InverterMode;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.server.Console;
import uk.co.stikman.log.StikLog;
import uk.co.stikman.table.DataTable;

@ModType(InvModType.INVERTER)
public class InverterPIPMAX extends InverterMonitor {
	private static final StikLog	LOGGER	= StikLog.getLogger(InverterPIPMAX.class);
	private PIP8048MAX				inv;
	private SerialPort				port;

	public InverterPIPMAX(String id, Env env) {
		super(id, env);
	}

	@Override
	public Sample createDataPoint(long ts) {
		DeviceStatus sts;
		synchronized (inv) {
			sts = inv.getStatus();
		}
		float current = sts.getBatteryChargeI();
		boolean charging = true;
		if (sts.getBatteryDischargeI() > current) {
			charging = false;
			current = sts.getBatteryDischargeI();
		}

		float pf = sts.getOutputApparentP() == 0 ? 0.0f : (float) sts.getOutputActiveP() / sts.getOutputApparentP();

		Sample dp = new Sample(ts);
		dp.put("mode", charging ? InverterMode.CHARGING : InverterMode.DISCHARGING);
		dp.put("chargeState", sts.getBatteryChargeStage());
		dp.put("battI", charging ? sts.getBatteryChargeI() : -sts.getBatteryDischargeI());
		dp.put("battV", sts.getBatteryV());
		float maxp = Math.max(sts.getOutputActiveP(), sts.getOutputApparentP());
		dp.put("loadI", maxp / sts.getOutputV());
		dp.put("loadV", sts.getOutputV());
		dp.put("loadF", sts.getOutputF());
		dp.put("pv1", sts.getPv1V(), sts.getPv1I(), 0);
		dp.put("pv2", sts.getPv2V(), sts.getPv2I(), 0);
		dp.put("temp", sts.getInverterTemp());
		dp.put("busV", sts.getBusV());
		dp.put("loadPF", pf);
		dp.put("soc", (float) sts.getBatteryCapacity() / 100.0f);
		dp.put("gridV", sts.getGridV());
		dp.put("gridI", sts.getGridI());
		dp.put("gridF", sts.getGridF());

		return dp;
	}

	@Override
	public void configure(Element config) {
		String portName = InvUtil.getAttrib(config, "port");
		this.port = null;
		for (SerialPort p : SerialPort.getCommPorts()) {
			if (p.getSystemPortName().equals(portName))
				this.port = p;
		}
		if (port == null) {
			//
			// print a list of serial ports in a nice table
			//
			LOGGER.error("Cannot find port [" + portName + "]");
			LOGGER.info("Available ports:");
			DataTable dt = new DataTable();
			dt.addFields("Num", "Port", "Path", "Description");
			int i = 0;
			SerialPort[] ports = SerialPort.getCommPorts();
			for (SerialPort x : ports)
				dt.addRecord(Integer.toString(++i), x.getSystemPortName(), x.getSystemPortPath(), x.getDescriptivePortName());
			for (String s : dt.toString().split("\\R"))
				LOGGER.info(s);

			throw new NoSuchElementException("Cannot find port [" + portName + "]");
		}

		//
		// create inverter interface type
		//
		String s = InvUtil.getAttrib(config, "type");
		try {
			Class<?> cls = Class.forName("uk.co.stikman.invmon.inverter." + s + "." + s);
			inv = (PIP8048MAX) cls.getConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Could not configure inverter type [" + s + "]: " + e.getMessage(), e);
		}

	}

	@Override
	public void start() throws InvMonException {
		super.start();
		inv.open(port);
	}

	@Override
	public void terminate() {
		inv.close();
		super.terminate();
	}

	@Override
	public void setOutputMode(OutputMode mode) throws InvMonException {
		synchronized (inv) {
			try {
				inv.setOutputMode(mode);
			} catch (Exception e) {
				throw new InvMonException(e);
			}
		}
	}

	@Override
	public ConsoleResponse consoleCommand(Console console, String cmd) throws InvMonException {
		if (cmd.equals("qpigs"))
			return new ConsoleResponse(inv.getLastQPIGS());

		try {
			if (cmd.startsWith("set float voltage")) {
				String s = cmd.substring(18);
				float v = Float.parseFloat(s);
				synchronized (inv) {
					inv.setFloatVoltage(v);
				}
				return new ConsoleResponse("OK");
			}
			
			if (cmd.startsWith("set bulk voltage")) {
				String s = cmd.substring(17);
				float v = Float.parseFloat(s);
				synchronized (inv) {
					inv.setBulkVoltage(v);
				}
				return new ConsoleResponse("OK");
			}

			if (cmd.startsWith("set output prio ")) {
				String s = cmd.substring(16);
				synchronized (inv) {
					inv.setOutputMode(OutputMode.fromShort(s));
				}
				return new ConsoleResponse("OK");
			}

			if (cmd.equals("info")) {
				synchronized (inv) {
					DeviceStatus ds = inv.getStatus();
					return new ConsoleResponse(ds.toString());
				}
			}

			if (cmd.equals("settings")) {
				synchronized (inv) {
					DeviceRatingInfo x = inv.getDeviceRatingInfo();
					return new ConsoleResponse(x.toString());
				}
			}

		} catch (IOException e) {
			throw new InvMonException(e);
		}

		return super.consoleCommand(console, cmd);
	}

	@Override
	protected void populateCommandHelp(List<ConsoleHelpInfo> lst) {
		super.populateCommandHelp(lst);
		lst.add(new ConsoleHelpInfo("qpigs", "show the last QPIGS response received"));
		lst.add(new ConsoleHelpInfo("set float voltage [v]", "set the float charge voltage"));
		lst.add(new ConsoleHelpInfo("set bulk voltage [v]", "set the bulk charge voltage"));
		lst.add(new ConsoleHelpInfo("set output prio [mode]", "set output priority to SBU or USB"));
		lst.add(new ConsoleHelpInfo("info", "read QPIGS datafrom inverter (current status)"));
		lst.add(new ConsoleHelpInfo("settings", "read QPIRI settings (mostly settings)"));
	}

}
