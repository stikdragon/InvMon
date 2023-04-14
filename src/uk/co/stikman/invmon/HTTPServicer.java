package uk.co.stikman.invmon;

import uk.co.stikman.invmon.server.InvMonHTTPResponse;

public interface HTTPServicer {
	InvMonHTTPResponse serve(InvMonHTTPRequest http);
}
