package uk.co.stikman.invmon;

import uk.co.stikman.invmon.htmlout.InvMonHTTPResponse;

public interface HTTPServicer {
	InvMonHTTPResponse serve(InvMonHTTPRequest http);
}
