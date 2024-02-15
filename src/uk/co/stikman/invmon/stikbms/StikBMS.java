package uk.co.stikman.invmon.stikbms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.function.Consumer;

import org.w3c.dom.Element;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.ConsoleHelpInfo;
import uk.co.stikman.invmon.ConsoleResponse;
import uk.co.stikman.invmon.ConsoleResponseStatus;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.Sample;
import uk.co.stikman.invmon.datalog.IntRange;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.inverter.util.TextPainter;
import uk.co.stikman.invmon.inverter.util.TextPainter.BoxSet;
import uk.co.stikman.invmon.server.Console;
import uk.co.stikman.log.StikLog;
import uk.co.stikman.table.DataRecord;
import uk.co.stikman.table.DataTable;

public class StikBMS extends InvModule {
	private static final StikLog	LOGGER			= StikLog.getLogger(StikBMS.class);
	private SerialPort				port;
	private int						baud;
	private List<BatteryData>		batteryData		= new ArrayList<>();
	private float[]					tempSensors;
	private int						numBatteries	= 1;
	private int						cellsPerBatt	= 16;
	private Lock					lock			= new ReentrantLock();

	public StikBMS(String id, Env env) {
		super(id, env);
	}

	@Subscribe(Events.POLL_SOURCES)
	public void poll(PollData data) throws InvMonException {
		synchronized (this) {
			try {
				StikBMSInterface bms = createBMS();
				try {
					BMSMetrics m = bms.queryMetrics();
					//
					// so this returns N cell voltages, we need to divide that up into the banks
					//
					if (m.getVoltages().length != numBatteries * cellsPerBatt)
						throw new InvMonException("Incorrect cell count received from BMS: " + m.getVoltages().length + ". Expected " + numBatteries * cellsPerBatt);

					for (int i = 0; i < m.getVoltages().length; ++i) {
						BatteryData b = batteryData.get(i / cellsPerBatt);
						float[] cells = b.getCellVoltages();
						cells[i % cellsPerBatt] = m.getVoltages()[i];
						b.setTemperature(0.0f);
						if (m.getTemperatures().length > 0)
							b.setTemperature(m.getTemperatures()[0]);
						b.setCurrent(0.0f); // how do we do this
					}

					for (float f : m.getVoltages()) {
						if (f > 0.5f)
							System.out.print(String.format("%05.2f ", f));
						else
							System.out.print("  -   ");
					}
					System.out.println();
					//
					// that's absolute voltages, so now we need to turn them into relative ones
					//

					for (BatteryData b : batteryData) {
						float[] cells = b.getCellVoltages();
						b.setPackVoltage(cells[cellsPerBatt - 1]);
						for (int i = cells.length - 1; i > 0; --i)
							cells[i] = cells[i] - cells[i - 1];
					}

					//
					// since we only want to present as a single battery we'll average them out
					// or pick the first one
					//
					Sample dp = new Sample(data.getTimestamp());
					BatteryData b = batteryData.get(0);
					for (int i = 0; i < cellsPerBatt; ++i)
						dp.put("c" + (i + 1), b.getCellVoltages()[i]);
					dp.put("i", m.getCurrent());
					dp.put("temp", b.getTemperature());
					dp.put("soc", 50.0f); // TODO: how do we estimate this?
					dp.put("v", b.getPackVoltage());

					data.add(getId(), dp);

				} finally {
					releaseBMS(bms);
				}
			} catch (Exception e) {
				throw new InvMonException("Failed to poll StikBMS: " + e.getMessage(), e);
			}
		}
	}

	@Override
	public void configure(Element config) throws InvMonException {
		numBatteries = Integer.parseInt(InvUtil.getAttrib(config, "batteryCount", "1"));
		cellsPerBatt = Integer.parseInt(InvUtil.getAttrib(config, "cellCount", "16"));
		baud = Integer.parseInt(InvUtil.getAttrib(config, "baud", "9600"));

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
	}

	@Override
	public void start() throws InvMonException {
		super.start();
		//
		// have a few goes at connecting because the recv buffer at the other end could
		// be in any old state that we don't know about
		//
		Exception fin = null;
		int v = -1;
		for (int i = 0; i < 3; ++i) {
			try {
				StikBMSInterface bms = createBMS();
				try {
					v = bms.queryProtocol();
				} finally {
					releaseBMS(bms);
				}
			} catch (Exception e) {
				LOGGER.warn("Error connecting to BMS: " + e.getMessage());
				LOGGER.info("  trying again...");
				fin = e;
			}
		}
		if (fin != null)
			throw new InvMonException(fin);
		if (v != 1)
			throw new InvMonException("Can only connect to v0.1 StikBMS unit");

		batteryData = new ArrayList<>();
		for (int i = 0; i < numBatteries; ++i)
			batteryData.add(new BatteryData(i, cellsPerBatt));
	}

	private StikBMSInterface createBMS() throws IOException {
		lock.lock();
		StikBMSInterface x = new StikBMSSerial(port, baud);
		//		StikBMSInterface x = new StikBMSFakeImpl(port, baud);
		x.open();
		return x;
	}

	private void releaseBMS(StikBMSInterface bms) throws IOException {
		bms.close();
		lock.unlock();
	}

	@Override
	public void terminate() {
		super.terminate();
	}

	public SerialPort getPort() {
		return port;
	}

	public List<BatteryData> getBatteryData() {
		return batteryData;
	}

	public int getNumBatteries() {
		return numBatteries;
	}

	public int getCellsPerBatt() {
		return cellsPerBatt;
	}

	@Override
	public ConsoleResponse consoleCommand(Console console, String cmd) throws InvMonException {
		try {
			if (cmd.equals("info"))
				return runConsoleInfo(console, cmd);
			if (cmd.startsWith("calib"))
				return runCalibStuff(console, cmd);
		} catch (Exception e) {
			return new ConsoleResponse(ConsoleResponseStatus.ERROR, e.toString(), false);
		}
		return super.consoleCommand(console, cmd);
	}

	private ConsoleResponse runCalibStuff(Console console, String cmd) throws IOException {
		if (cmd.equals("calib"))
			return new ConsoleResponse("""
					^3CALIBRATE CELL VOLTAGES^x:
					========================
					    ^5calib cell reset
					    calib cell all ^2[voltage]^5
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

					You can do this cell-by-cell as well, if you want.  Cells start at index ^50^x.

					    ^5calib cell 0=13.34, 5=16.66, 6=20.02^x

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

		if (cmd.equals("calib show")) {
			StikBMSInterface bms = createBMS();
			try {
				DataTable dt = new DataTable();
				dt.addFields("Factor", "Value");
				for (CalibFactor x : bms.getCalibFactors())
					dt.addRecord(x.getName(), String.format("%.5f", x.getValue()));
				return new ConsoleResponse(dt.toString());
			} finally {
				releaseBMS(bms);
			}
		}

		if (cmd.equals("calib reset")) {
			StikBMSInterface bms = createBMS();
			try {
				bms.resetCalib();
				return new ConsoleResponse("OK");
			} finally {
				releaseBMS(bms);
			}
		}

		//
		// urgh this could be more elegant
		//
		if (cmd.startsWith("calib cell all ")) {
			String val = cmd.substring("calib cell all ".length()).trim();
			if (val.endsWith("v") || val.endsWith("V"))
				val = val.substring(0, val.length() - 1);
			Float f = Float.parseFloat(val);
			List<IFPair> lst = new ArrayList<>();
			for (int i = 0; i < numBatteries * cellsPerBatt; ++i)
				lst.add(new IFPair(i, f));
			StringBuffer sb = new StringBuffer();
			applyCalibrationFactors(CalibTarget.VOLTAGE, lst, s -> sb.append(s).append("\n"));
			return new ConsoleResponse(sb.toString());
		} else if (cmd.startsWith("calib raw all ")) {
			String val = cmd.substring("calib raw all ".length()).trim();
			if (val.endsWith("v") || val.endsWith("V"))
				val = val.substring(0, val.length() - 1);
			Float f = Float.parseFloat(val);
			List<IFPair> lst = new ArrayList<>();
			for (int i = 0; i < numBatteries * cellsPerBatt; ++i)
				lst.add(new IFPair(i, f));
			StringBuffer sb = new StringBuffer();
			applyCalibrationFactors(CalibTarget.VOLTAGE_RAW, lst, s -> sb.append(s).append("\n"));
			return new ConsoleResponse(sb.toString());
		} else if (cmd.startsWith("calib cell ")) {
			String val = cmd.substring(11).replaceAll(" *", "");
			String bits[] = val.split(",");
			List<IFPair> lst = new ArrayList<>();
			for (String bit : bits) {
				String[] ab = bit.split("=");
				int cell = Integer.parseInt(ab[0]);
				float volt = Float.parseFloat(ab[1]);
				lst.add(new IFPair(cell, volt));
			}
			StringBuffer sb = new StringBuffer();
			applyCalibrationFactors(CalibTarget.VOLTAGE, lst, s -> sb.append(s).append("\n"));
			return new ConsoleResponse(sb.toString());
		} else if (cmd.startsWith("calib raw ")) {
			String val = cmd.substring(10).replaceAll(" *", "");
			String bits[] = val.split(",");
			List<IFPair> lst = new ArrayList<>();
			for (String bit : bits) {
				String[] ab = bit.split("=");
				int cell = Integer.parseInt(ab[0]);
				float volt = Float.parseFloat(ab[1]);
				lst.add(new IFPair(cell, volt));
			}
			StringBuffer sb = new StringBuffer();
			applyCalibrationFactors(CalibTarget.VOLTAGE_RAW, lst, s -> sb.append(s).append("\n"));
			return new ConsoleResponse(sb.toString());
		} else if (cmd.startsWith("calib offset ")) {
			String val = cmd.substring("calib offset ".length()).trim();
			if (val.endsWith("v") || val.endsWith("V"))
				val = val.substring(0, val.length() - 1);
			Float f = Float.parseFloat(val);
			List<IFPair> lst = new ArrayList<>();
			lst.add(new IFPair(0, f));
			StringBuffer sb = new StringBuffer();
			applyCalibrationFactors(CalibTarget.OFFSET, lst, s -> sb.append(s).append("\n"));
			return new ConsoleResponse(sb.toString());
		}

		return new ConsoleResponse("what");

	}

	/**
	 * expect the indexes in <code>lst</code> to be 0-based. if <code>log</code> is
	 * not null it'll log stuff to it
	 * 
	 * @param tgt
	 * @param lst
	 * @throws IOException
	 */
	private void applyCalibrationFactors(CalibTarget tgt, List<IFPair> lst, Consumer<String> log) throws IOException {
		if (log == null)
			log = s -> {
			};
		StikBMSInterface bms = createBMS();
		try {
			//
			// get the current reported voltages and work out how much to adjust
			// them to get the measured voltage
			//
			BMSMetrics cur = bms.queryMetrics();
			int count = 0;
			for (IFPair p : lst) {
				float f = p.getF();
				if (tgt == CalibTarget.VOLTAGE) {
					float v = cur.getVoltages()[p.getI()];
					if (v <= 0.01f) { // let's avoid getting absurdly large factors here
						log.accept("WARN: Cell [" + (p.getI() + 1) + "] reports 0.0v, skipping calib for this one");
						continue;
					}
					f = p.getF() / v;
				}

				bms.setCalibFactor(tgt, p.getI(), f);
				++count;
			}
			log.accept("Set [" + count + "] calib factors.");
		} finally {
			releaseBMS(bms);
		}

	}

	private ConsoleResponse runConsoleInfo(Console console, String cmd) {
		TextPainter tp = new TextPainter();

		final int W = 24;
		tp.putColour(1, 0, "#", '3');
		tp.putColour(5, 0, "CellV", '3');
		tp.putColour(13, 0, "AbsV", '3');

		tp.putColour(W + 1, 0, "#", '3');
		tp.putColour(W + 5, 0, "CellV", '3');
		tp.putColour(W + 13, 0, "AbsV", '3');
		for (int x = 0; x < 2; ++x) {
			BatteryData bi = batteryData.get(x);
			for (int y = 0; y < 16; ++y) {
				float va = bi.getCellVoltages()[y];
				float dv = bi.getCellVoltages()[y];
				tp.putColour(x * W, y + 1, String.format("%2d:", x * 16 + y), '1');
				tp.putColour(x * W + 4, y + 1, String.format("%5.02f", dv), '5');
				tp.putColour(x * W + 9, y + 1, "V", '4');
				tp.putColour(x * W + 11, y + 1, String.format("%5.02f", va), '1');
				tp.putColour(x * W + 16, y + 1, "V", '1');
			}
		}

		ConsoleResponse res = new ConsoleResponse(tp.toString(true));
		return res;
	}

	@Override
	protected void populateCommandHelp(List<ConsoleHelpInfo> lst) {
		super.populateCommandHelp(lst);
		lst.add(new ConsoleHelpInfo("info", "show latest data received"));
		lst.add(new ConsoleHelpInfo("calib", "set calibration factors for the BMS. Type 'calib' with no arguments for help"));
	}

	@Override
	public String toString() {
		return "Port=" + port.getSystemPortName() + " @ " + baud + "\nCellsPerBatt=" + cellsPerBatt + ",  Batteries=" + numBatteries;
	}

}
