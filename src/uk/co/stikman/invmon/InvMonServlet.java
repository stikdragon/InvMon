package uk.co.stikman.invmon;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class InvMonServlet extends HttpServlet {

	private static final long		serialVersionUID	= 1L;
	private static InvMonServlet	INSTANCE;
	private HTTPServicer			intf;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doGet(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doPost(req, resp);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		//
		// this isn't very nice, sorry
		//
		INSTANCE = this;
	}

	public static boolean isActive() {
		return INSTANCE != null;
	}

	public static void setInterface(HTTPServicer intf) {
		INSTANCE.intf = intf;
	}

}
