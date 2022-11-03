package uk.co.stikman.invmon.inverter.PIP8048MAX;

import java.util.NoSuchElementException;

import org.w3c.dom.Element;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.DataPoint;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.datamodel.FieldVIF;
import uk.co.stikman.invmon.datamodel.InverterMode;
import uk.co.stikman.invmon.inverter.InvUtil;
import uk.co.stikman.log.StikLog;
import uk.co.stikman.table.DataTable;

public class InverterMonitorPIP extends InvModule {
	private static final StikLog	LOGGER	= StikLog.getLogger(InverterMonitorPIP.class);
	private PIP8048MAX				inv;
	private SerialPort				port;

	private Field					fieldMode;
	private Field					fieldChargeState;
	private FieldVIF				fieldBattery;
	private FieldVIF				fieldLoad;
	private FieldVIF				fieldPv1;
	private FieldVIF				fieldPv2;
	private Field					fieldTemperature;
	private Field					fieldBusVoltage;
	private Field					fieldLoadPF;
	private Field					fieldStateOfCharge;
	private Field					fieldMisc;
	private Field					fieldPv1P;
	private Field					fieldPv2P;

	public InverterMonitorPIP(String id, Env env) {
		super(id, env);
	}

	@Subscribe(Events.POLL_SOURCES)
	public void poll(PollData data) {
		DeviceStatus sts = inv.getStatus();
		float current = sts.getBatteryChargeI();
		boolean charging = true;
		if (sts.getBatteryDischargeI() > current) {
			charging = false;
			current = sts.getBatteryDischargeI();
		}

		float pf = sts.getOutputApparentP() == 0 ? 0.0f : (float) sts.getOutputActiveP() / sts.getOutputApparentP();

		DataPoint dp = new DataPoint(data.getTimestamp());
		data.add(getId(), dp);

		dp.put(fieldMode, charging ? InverterMode.CHARGING : InverterMode.DISCHARGING);
		dp.put(fieldChargeState, sts.getBatteryChargeStage());
		dp.put(fieldBattery, sts.getBatteryV(), charging ? sts.getBatteryChargeI() : -sts.getBatteryDischargeI(), 0);
		float maxp = Math.max(sts.getOutputActiveP(), sts.getOutputApparentP());
		dp.put(fieldLoad, sts.getOutputV(), maxp / sts.getOutputV(), sts.getOutputF());
		dp.put(fieldPv1, sts.getPv1V(), sts.getPv1I(), 0);
		dp.put(fieldPv2, sts.getPv2V(), sts.getPv2I(), 0);
		dp.put(fieldPv1P, sts.getPv1V() * sts.getPv1I());
		dp.put(fieldPv2P, sts.getPv2V() * sts.getPv2I());
		dp.put(fieldTemperature, sts.getInverterTemp());
		dp.put(fieldBusVoltage, sts.getBusV());
		dp.put(fieldLoadPF, pf);
		dp.put(fieldStateOfCharge, (float) sts.getBatteryCapacity() / 100.0f);
		dp.put(fieldMisc, sts.getDeviceStatus() + " / " + sts.getDeviceStatus2());

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

		DataModel model = getEnv().getModel();
		fieldMode = model.get("INV_MODE");
		fieldChargeState = model.get("BATT_MODE");
		fieldBattery = model.getVIF("BATT");
		fieldLoad = model.getVIF("BATT");
		fieldPv1 = model.getVIF("PV1");
		fieldPv2 = model.getVIF("PV2");
		fieldPv1P = model.get("PV1_P");
		fieldPv2P = model.get("PV2_P");
		fieldTemperature = model.get("INV_TEMP");
		fieldBusVoltage = model.get("INV_BUS_V");
		fieldLoadPF = model.get("LOAD_PF");
		fieldStateOfCharge = model.get("BATT_SOC");
		fieldMisc = model.get("MISC");
	}

	@Override
	public void terminate() {
		super.terminate();
	}

}
