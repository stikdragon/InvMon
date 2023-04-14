package uk.co.stikman.invmon.server;

import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datalog.QueryResults;

public class WidgetExecuteContext {
	private final HTTPServer	owner;
	private final UserSesh		session;
	private final QueryResults	results;
	private final DBRecord		mostRecent;

	public WidgetExecuteContext(HTTPServer owner, UserSesh sesh, QueryResults results, DBRecord mostRecent) {
		super();
		this.owner = owner;
		this.session = sesh;
		this.results = results;
		this.mostRecent = mostRecent;
	}

	public QueryResults getResults() {
		return results;
	}

	public DBRecord getMostRecent() {
		return mostRecent;
	}

	@Override
	public String toString() {
		return "QueryResultsAndRecent [results=" + results + ", mostRecent=" + mostRecent + "]";
	}

	public UserSesh getSession() {
		return session;
	}

	public HTTPServer getOwner() {
		return owner;
	}
}
