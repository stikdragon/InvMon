package uk.co.stikman.invmon;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.inverter.PIP8048MAX.OutputMode;

public abstract class InverterMonitor extends InvModule {

	public InverterMonitor(String id, Env env) {
		super(id, env);
	}

	public abstract Sample createDataPoint(long ts);

	@Subscribe(Events.POLL_SOURCES)
	public void poll(PollData data) {
		synchronized (this) {
			Sample dp = createDataPoint(data.getTimestamp());
			data.add(getId(), dp);
		}
	}

	public abstract void setOutputMode(OutputMode mode) throws InvMonException;

}
