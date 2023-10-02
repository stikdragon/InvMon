package uk.co.stikman.invmon.server;

public class WebUtils {

	/**
	 * keys for associating objects with a user session. in particular the
	 * cached ones are used to avoid re-querying the database for multiple hits
	 * with a single page
	 */
	public static final String	GLOBAL_VIEW_OPTIONS		= "gvo";
	public static final String	CACHED_QUERY_RESULTS	= "cqr";
	public static final String	CACHED_LAST_RECORD		= "clr";

}
