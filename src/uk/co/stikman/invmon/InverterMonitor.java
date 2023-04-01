package uk.co.stikman.invmon;

import uk.co.stikman.eventbus.Subscribe;

public abstract class InverterMonitor extends InvModule {

	public InverterMonitor(String id, Env env) {
		super(id, env);
	}

	/**
	 * a grouped inverter should not respond to POLL_SOURCES
	 * 
	 * @param b
	 */
	public abstract void setGrouped(boolean b);
	public abstract boolean isGrouped();
	public abstract Sample createDataPoint(long ts);

	@Subscribe(Events.POLL_SOURCES)
	public void poll(PollData data) {
		if (isGrouped())
			return;
		Sample dp = createDataPoint(data.getTimestamp());
		data.add(getId(), dp);
	}
	
}
