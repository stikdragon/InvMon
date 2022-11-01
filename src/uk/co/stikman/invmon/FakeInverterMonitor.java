package uk.co.stikman.invmon;

import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.inverter.BatteryChargeStage;
import uk.co.stikman.log.StikLog;

public class FakeInverterMonitor extends ProcessPart {
	private static final StikLog LOGGER = StikLog.getLogger(FakeInverterMonitor.class);

	public FakeInverterMonitor(String id, Env env) {
		super(id, env);
	}

	@Subscribe(Events.POLL_SOURCES)
	public void poll(PollData data) {
		InverterDataPoint rec = new InverterDataPoint();
		rec.setMode(InverterMode.CHARGING);
		rec.setChargeState(BatteryChargeStage.CHARGE_FLOAT);
		rec.setBattery(new VIFReading(50.2f + rand(10f), 24.0f + rand(10f), 0f));
		rec.setLoad(new VIFReading(230.0f, 1.45f + rand(3f), 50.0f));
		rec.setPvCount(2);
		rec.setPv(0, new VIFReading(304f + rand(40f), 4.0f + rand(7f)));
		rec.setPv(1, new VIFReading(155f + rand(30f), 3.4f + rand(6f)));
		rec.setTemperature(41f + rand(5f));
		rec.setBusVoltage((int) (380 + rand(100f)));
		rec.setLoadPF(0.95f + rand(0.50f));
		rec.setStateOfCharge(0.52f + rand(0.5f));
		rec.setMisc("misc");
		data.add(getId(), rec);

	}

	private float rand(float f) {
		return (float) ((0.5f - Math.random()) * f);
	}

	@Override
	public void configure(Element config) {
	}

	@Override
	public void start() throws InvMonException {
		super.start();
	}

	@Override
	public void terminate() {
		super.terminate();
	}

}
