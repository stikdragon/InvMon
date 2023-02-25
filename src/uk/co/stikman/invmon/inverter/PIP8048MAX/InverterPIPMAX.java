package uk.co.stikman.invmon.inverter.PIP8048MAX;

import java.util.NoSuchElementException;

import org.w3c.dom.Element;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.stikman.invmon.DataPoint;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.InverterMonitor;
import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.datamodel.FieldVIF;
import uk.co.stikman.invmon.datamodel.InverterMode;
import uk.co.stikman.invmon.inverter.InvUtil;
import uk.co.stikman.log.StikLog;
import uk.co.stikman.table.DataTable;

public class InverterPIPMAX extends InverterMonitor {
	private static final StikLog	LOGGER	= StikLog.getLogger(InverterPIPMAX.class);
	private PIP8048MAX				inv;
	private SerialPort				port;

	private Field					fieldMode;
	private Field					fieldChargeState;
	private FieldVIF				fieldPv1;
	private FieldVIF				fieldPv2;
	private Field					fieldTemperature;
	private Field					fieldBusVoltage;
	private Field					fieldLoadPF;
	private Field					fieldStateOfCharge;
	private Field					fieldPv1P;
	private Field					fieldPv2P;
	private Field					fieldLoadV;
	private Field					fieldLoadI;
	private Field					fieldBattV;
	private Field					fieldBattI;

	private boolean					grouped;

	public InverterPIPMAX(String id, Env env) {
		super(id, env);
	}

	@Override
	public DataPoint createDataPoint(long ts) {
		DeviceStatus sts = inv.getStatus();
		float current = sts.getBatteryChargeI();
		boolean charging = true;
		if (sts.getBatteryDischargeI() > current) {
			charging = false;
			current = sts.getBatteryDischargeI();
		}

		float pf = sts.getOutputApparentP() == 0 ? 0.0f : (float) sts.getOutputActiveP() / sts.getOutputApparentP();

		DataPoint dp = new DataPoint(ts);

		dp.put(fieldMode, charging ? InverterMode.CHARGING : InverterMode.DISCHARGING);
		dp.put(fieldChargeState, sts.getBatteryChargeStage());
		dp.put(fieldBattI, charging ? sts.getBatteryChargeI() : -sts.getBatteryDischargeI());
		dp.put(fieldBattV, sts.getBatteryV());
		float maxp = Math.max(sts.getOutputActiveP(), sts.getOutputApparentP());
		dp.put(fieldLoadI, maxp / sts.getOutputV());
		dp.put(fieldLoadV, sts.getOutputV());
		dp.put(fieldPv1, sts.getPv1V(), sts.getPv1I(), 0);
		dp.put(fieldPv2, sts.getPv2V(), sts.getPv2I(), 0);
		dp.put(fieldPv1P, sts.getPv1V() * sts.getPv1I());
		dp.put(fieldPv2P, sts.getPv2V() * sts.getPv2I());
		dp.put(fieldTemperature, sts.getInverterTemp());
		dp.put(fieldBusVoltage, sts.getBusV());
		dp.put(fieldLoadPF, pf);
		dp.put(fieldStateOfCharge, (float) sts.getBatteryCapacity() / 100.0f);
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
		DataModel model = getEnv().getModel();
		fieldMode = model.get("INV_MODE");
		fieldChargeState = model.get("BATT_MODE");
		fieldBattV = model.get("BATT_V");
		fieldBattI = model.get("INV_1_I");
		fieldLoadV = model.get("LOAD_V");
		fieldLoadI = model.get("LOAD_1_I");
		fieldLoadPF = model.get("LOAD_PF");
		fieldPv1 = model.getVIF("PVA_1");
		fieldPv2 = model.getVIF("PVB_1");
		fieldPv1P = model.get("PVA_1_P");
		fieldPv2P = model.get("PVB_1_P");
		fieldTemperature = model.get("INV_1_TEMP");
		fieldBusVoltage = model.get("INV_1_BUS_V");
		fieldStateOfCharge = model.get("BATT_SOC");
	}

	@Override
	public void terminate() {
		inv.close();
		super.terminate();
	}

	@Override
	public void setGrouped(boolean b) {
		this.grouped = b;
	}

	@Override
	public boolean isGrouped() {
		return grouped;
	}

}
