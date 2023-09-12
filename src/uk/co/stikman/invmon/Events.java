package uk.co.stikman.invmon;

public class Events {

	public static final String	POLL_SOURCES			= "poll_sources";
	public static final String	POST_DATA				= "post_data";
	public static final String	LOGGER_RECORD_COMMITED	= "logger_commit";

	public static final String	AUTOREFRESH_CHANGED		= "autorefresh_changed";
	public static final String	REFRESH_NOW				= "refresh_now";

	/**
	 * fires once per loop
	 */
	public static final String	TIMER_UPDATE_PERIOD		= "timer1";

	/**
	 * fires once per minute
	 */
	public static final String	TIMER_UPDATE_MINUTE		= "timer2";

}
