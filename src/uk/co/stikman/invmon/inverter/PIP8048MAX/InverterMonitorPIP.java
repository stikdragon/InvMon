package uk.co.stikman.invmon.inverter.PIP8048MAX;

import java.util.NoSuchElementException;

import org.w3c.dom.Element;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.InverterDataPoint;
import uk.co.stikman.invmon.InverterMode;
import uk.co.stikman.invmon.ProcessPart;
import uk.co.stikman.invmon.VIFReading;
import uk.co.stikman.invmon.inverter.InverterUtils;
import uk.co.stikman.log.StikLog;
import uk.co.stikman.table.DataTable;

public class InverterMonitorPIP implements ProcessPart {
	private static final StikLog	LOGGER		= StikLog.getLogger(InverterMonitorPIP.class);
	private PIP8048MAX				inv;
	private Thread					monitorThread;
	private SerialPort				port;
	private boolean					terminate	= false;
	private Env						env;
	private String					id;

	public InverterMonitorPIP(String id, Env env) {
		this.id = id;
		this.env = env;
	}

	private void loop() {
		LOGGER.info("Starting InverterMonitor");
		inv.open(port);
		try {
			for (;;) {
				if (terminate)
					return;
				DeviceStatus sts = inv.getStatus();
				float current = sts.getBatteryChargeI();
				boolean charging = true;
				if (sts.getBatteryDischargeI() > current) {
					charging = false;
					current = sts.getBatteryDischargeI();
				}

				float pf = sts.getOutputApparentP() == 0 ? 0.0f : (float) sts.getOutputActiveP() / sts.getOutputApparentP();

				InverterDataPoint rec = new InverterDataPoint();
				rec.setMode(charging ? InverterMode.CHARGING : InverterMode.DISCHARGING);
				rec.setChargeState(sts.getBatteryChargeStage());
				rec.setBattery(new VIFReading(sts.getBatteryV(), charging ? sts.getBatteryChargeI() : -sts.getBatteryDischargeI()));
				float maxp = Math.max(sts.getOutputActiveP(), sts.getOutputApparentP());
				rec.setLoad(new VIFReading(sts.getOutputV(), maxp / sts.getOutputV(), sts.getOutputF()));
				rec.setPvCount(2);
				rec.setPv(0, new VIFReading(sts.getPv1V(), sts.getPv1I()));
				rec.setPv(1, new VIFReading(sts.getPv2V(), sts.getPv2I()));
				rec.setTemperature(sts.getInverterTemp());
				rec.setBusVoltage(sts.getBusV());
				rec.setLoadPF(pf);
				rec.setStateOfCharge((float) sts.getBatteryCapacity() / 100.0f);
				rec.setMisc(sts.getDeviceStatus() + " / " + sts.getDeviceStatus2());

				env.postRecord(rec);

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					if (terminate)
						return;
				}
			}

		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			inv.close();
		}
	}

	@Override
	public void configure(Element config) {
		String portName = InverterUtils.getAttrib(config, "port");
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
		String s = InverterUtils.getAttrib(config, "type");
		try {
			Class<?> cls = Class.forName("uk.co.stikman.invmon.inverter." + s + "." + s);
			inv = (PIP8048MAX) cls.getConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Could not configure inverter type [" + s + "]: " + e.getMessage(), e);
		}

	}

	@Override
	public void start() {
		if (monitorThread != null)
			throw new IllegalStateException("Already started");
		monitorThread = new Thread(this::loop);
		monitorThread.start();
	}

	@Override
	public void terminate() {
		this.terminate = true;
		if (monitorThread != null)
			monitorThread.interrupt();
	}

	@Override
	public String getId() {
		return id;
	}
}
