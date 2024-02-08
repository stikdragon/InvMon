package uk.co.stikman.invmon.server.widgets;

import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.FieldNameList;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.datalog.MiniDbException;
import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.minidom.MDElement;
import uk.co.stikman.invmon.server.PageLayout;
import uk.co.stikman.invmon.server.UserSesh;
import uk.co.stikman.invmon.server.ViewOptions;
import uk.co.stikman.invmon.server.WebUtils;
import uk.co.stikman.invmon.shared.WidgetConfigOptions;

public abstract class PageWidget {
	private final PageLayout	owner;
	private final DataLogger	datalogger;
	private String				id;
	private int					x;
	private int					y;
	private int					width;
	private int					height;
	private String				title;

	public PageWidget(PageLayout owner) {
		this.owner = owner;
		this.datalogger = owner.getEnv().getModule("datalogger");
	}

	public String getId() {
		return id;
	}

	public void configure(MDElement root) {
		id = root.getAttrib("id");
		String s = root.getAttrib("layout");
		String[] bits = s.split(",");
		x = Integer.parseInt(bits[0].trim());
		y = Integer.parseInt(bits[1].trim());
		width = Integer.parseInt(bits[2].trim());
		height = Integer.parseInt(bits[3].trim());
		title = root.getAttrib("title", null);

	}

	public void toDOM(MDElement root) {
		root.setAttrib("id", id);
		root.setAttrib("layout", String.format("%d, %d, %d, %d", x, y, width, height));
		root.setAttrib("title", title);
	}

	public abstract JSONObject executeApi(UserSesh sesh, String api, JSONObject args);

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public abstract String getClientWidgetType();

	public String getTitle() {
		return title;
	}

	protected void ensureCachedResults(UserSesh sesh) {
		synchronized (sesh) {
			ViewOptions opts = sesh.getData(WebUtils.GLOBAL_VIEW_OPTIONS);
			QueryResults qr = sesh.getData(WebUtils.CACHED_QUERY_RESULTS);
			if (qr == null) {
				long end = System.currentTimeMillis();
				long start = end - (long) opts.getDuration() * 1000 * 60;
				FieldNameList flds = new FieldNameList();
				//
				// add everything except timestamp, i guess
				//
				for (Field f : getOwner().getEnv().getModel())
					if (!f.getId().equals("TIMESTAMP"))
						flds.add(f.getId());

				try {
					QueryResults aggr = datalogger.query(start, end, 100, flds.asList());
					sesh.putData(WebUtils.CACHED_QUERY_RESULTS, aggr);
					sesh.putData(WebUtils.CACHED_LAST_RECORD, datalogger.getLastRecord());
				} catch (MiniDbException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	public PageLayout getOwner() {
		return owner;
	}

	public static ViewOptions getViewOpts(UserSesh sesh) {
		ViewOptions viewopts = sesh.getData(WebUtils.GLOBAL_VIEW_OPTIONS);
		if (viewopts == null)
			sesh.putData(WebUtils.GLOBAL_VIEW_OPTIONS, viewopts = new ViewOptions());
		return viewopts;
	}

	public DataLogger getDatalogger() {
		return datalogger;
	}
	
	public abstract WidgetConfigOptions getConfigOptions();
	public abstract void applyConfigOptions(WidgetConfigOptions opts);

}
