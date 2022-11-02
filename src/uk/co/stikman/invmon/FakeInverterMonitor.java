package uk.co.stikman.invmon;

import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.datamodel.FieldVIF;
import uk.co.stikman.invmon.datamodel.InverterMode;
import uk.co.stikman.invmon.inverter.BatteryChargeStage;
import uk.co.stikman.log.StikLog;

public class FakeInverterMonitor extends ProcessPart {
	private static final StikLog	LOGGER	= StikLog.getLogger(FakeInverterMonitor.class);

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
		fieldBattery = model.getVIF("BATT");
		fieldLoad = model.getVIF("BATT");
		fieldPv1 = model.getVIF("PV1");
		fieldPv2 = model.getVIF("PV2");
		fieldTemperature = model.get("INV_TEMP");
		fieldBusVoltage = model.get("INV_BUS_V");
		fieldLoadPF = model.get("LOAD_PF");
		fieldStateOfCharge = model.get("BATT_SOC");
		fieldMisc = model.get("MISC");
	}

	@Subscribe(Events.POLL_SOURCES)
	public void poll(PollData data) {
		DataPoint dp = new DataPoint(data.getTimestamp());
		data.add(getId(), dp);
		
		dp.put(fieldMode, InverterMode.CHARGING);
		dp.put(fieldChargeState,  BatteryChargeStage.CHARGE_FLOAT);
		dp.put(fieldBattery, 50.2f + rand(10f), 24.0f + rand(10f), 0f);
		dp.put(fieldLoad, 30.0f, 1.45f + rand(3f), 50.0f);
		dp.put(fieldPv1, 304f + rand(40f), 4.0f + rand(7f), 0);
		dp.put(fieldPv2, 304f + rand(40f), 4.0f + rand(7f), 0);
		dp.put(fieldTemperature, 41f + rand(5f));
		dp.put(fieldBusVoltage, (int)(380 + rand(100)));
		dp.put(fieldLoadPF, rand(1.0f));
		dp.put(fieldStateOfCharge, 0.52f + rand(0.5f));
		dp.put(fieldMisc, "misc");
	}

	private float rand(float f) {
		return (float) ((0.5f - Math.random()) * f);
	}

	@Override
	public void terminate() {
		super.terminate();
	}

}
