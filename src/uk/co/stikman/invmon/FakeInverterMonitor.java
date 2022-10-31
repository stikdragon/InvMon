package uk.co.stikman.invmon;

import org.w3c.dom.Element;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.stikman.invmon.inverter.BatteryChargeStage;
import uk.co.stikman.log.StikLog;

public class FakeInverterMonitor implements ProcessPart {
	private static final StikLog	LOGGER		= StikLog.getLogger(FakeInverterMonitor.class);
	private SerialPort				port;
	private boolean					terminate	= false;
	private Env						env;
	private String					id;
	private Thread					monitorThread;

	public FakeInverterMonitor(String id, Env env) {
		this.id = id;
		this.env = env;
	}

	private void loop() {
		LOGGER.info("Starting FakeInverterMonitor");
		try {
			for (;;) {
				if (terminate)
					return;
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
		}
	}

	private float rand(float f) {
		return (float) ((0.5f - Math.random()) * f);
	}

	@Override
	public void configure(Element config) {
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
