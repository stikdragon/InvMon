package uk.co.stikman.invmon;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.datamodel.FieldVIF;
import uk.co.stikman.invmon.datamodel.InverterMode;
import uk.co.stikman.invmon.inverter.BatteryChargeStage;
import uk.co.stikman.log.StikLog;

public class FakeInverterMonitor extends InverterMonitor {
	private static final StikLog	LOGGER	= StikLog.getLogger(FakeInverterMonitor.class);

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

	public FakeInverterMonitor(String id, Env env) {
		super(id, env);
	}

	@Override
	public void configure(Element config) {
	}

	@Override
	public void start() throws InvMonException {
		super.start();

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
	public DataPoint createDataPoint(long ts) {
		DataPoint dp = new DataPoint(ts);
		dp.put(fieldMode, InverterMode.CHARGING);
		dp.put(fieldChargeState, BatteryChargeStage.CHARGE_FLOAT);
		dp.put(fieldBattI, rand(90f));
		dp.put(fieldBattV, 50f + rand(20f));
		dp.put(fieldLoadI, 1.45f + rand(3f));
		dp.put(fieldLoadV, 230.0f + rand(10f));
		dp.put(fieldPv1, 304f + rand(40f), 4.0f + rand(7f), 0);
		dp.put(fieldPv2, 304f + rand(40f), 4.0f + rand(7f), 0);
		dp.put(fieldPv1P, rand(1000) + 500);
		dp.put(fieldPv2P, rand(1000) + 500);
		dp.put(fieldTemperature, 41f + rand(5f));
		dp.put(fieldBusVoltage, (int) (380 + rand(100)));
		dp.put(fieldLoadPF, rand(1.0f));
		dp.put(fieldStateOfCharge, 0.52f + rand(0.5f));
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
	public void setGrouped(boolean b) {
		this.grouped = b;
	}

	@Override
	public boolean isGrouped() {
		return grouped;
	}

}
