package uk.co.stikman.invmon.server.widgets;

import org.json.JSONObject;

import uk.co.stikman.invmon.FieldNameList;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.datalog.MiniDbException;
import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.datalog.stats.StatsThing;
import uk.co.stikman.invmon.datamodel.ModelField;
import uk.co.stikman.invmon.minidom.MDElement;
import uk.co.stikman.invmon.server.InvMonClientError;
import uk.co.stikman.invmon.server.PageLayout;
import uk.co.stikman.invmon.server.UserSesh;
import uk.co.stikman.invmon.server.ViewOptions;
import uk.co.stikman.invmon.server.WebUtils;
import uk.co.stikman.invmon.shared.WidgetConfigOptions;
import uk.co.stikman.log.StikLog;

public abstract class PageWidget {
	private static final StikLog	LOGGER	= StikLog.getLogger(PageWidget.class);

	private final PageLayout		owner;
	private final DataLogger		datalogger;
	private String					id;
	private int						x;
	private int						y;
	private int						width;
	private int						height;
	private String					title;

	public PageWidget(PageLayout owner) {
		this.owner = owner;
		this.datalogger = owner.getEnv().getModule("datalogger");
	}

	public String getId() {
		return id;
	}

	public void fromDOM(MDElement root) {
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

	public JSONObject executeApi(UserSesh sesh, String api, JSONObject args) {
		if ("getConfig".equals(api))
			return getConfig(args);
		else if ("setConfig".equals(api))
			return setConfig(sesh, args);
		else if ("setPosition".equals(api))
			return setPosition(args);

		throw new InvMonClientError("API [" + api + "] not supported");
	}

	private JSONObject setPosition(JSONObject args) {
		this.x = args.getInt("x");
		this.y = args.getInt("y");
		this.width = args.getInt("w");
		this.height = args.getInt("h");
		return new JSONObject();
	}

	private JSONObject setConfig(UserSesh sesh, JSONObject args) {
		WidgetConfigOptions co = new WidgetConfigOptions();
		co.fromJSON(args);
		applyConfigOptions(co);
		try {
			getOwner().saveToSource();
		} catch (InvMonException e) {
			LOGGER.error("Failed to write page config to disk: " + e.getMessage(), e);
		}
		return new JSONObject();
	}

	private JSONObject getConfig(JSONObject args) {
		WidgetConfigOptions co = getConfigOptions();
		JSONObject jo = new JSONObject();
		co.toJSON(jo);
		return jo;
	}

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
				for (ModelField f : getOwner().getEnv().getModel())
					if (!f.getId().equals("TIMESTAMP"))
						flds.add(f.getId());

				//
				// and any stats fields?
				//
				for (StatsThing thing : datalogger.getStatsDb().getThings()) {
					for (String s : thing.getOutputFields())
						flds.add(thing.getId() + "." + s);
				}

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
