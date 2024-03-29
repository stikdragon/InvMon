package uk.co.stikman.invmon.server;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonHTTPRequest;
import uk.co.stikman.invmon.client.res.ClientRes;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.datamodel.ModelField;
import uk.co.stikman.invmon.nanohttpd.NanoHTTPD.Response.Status;
import uk.co.stikman.log.StikLog;
import uk.co.stikman.table.CSVExporter;
import uk.co.stikman.table.DataRecord;
import uk.co.stikman.table.DataTable;

public class DataViewPage {
	private static final StikLog	LOGGER	= StikLog.getLogger(DataViewPage.class);
	private HTTPServer				owner;

	private enum Format {
		CSV, FIXED
	}

	public DataViewPage(HTTPServer owner) {
		this.owner = owner;
	}

	public InvMonHTTPResponse exec(String url, UserSesh sesh, InvMonHTTPRequest session) {
		try {

			if (session.getParameters().isEmpty())
				return showHelp();

			String s = session.optParam("dur", "1h");
			if (!s.matches("-?[0-9]+[smhdw]"))
				throw new IllegalArgumentException("'duration' is not correct");
			long dur = Integer.parseInt(s.substring(0, s.length() - 1));
			switch (s.charAt(s.length() - 1)) {
				case 'w':
					dur *= 7;
				case 'd':
					dur *= 24;
				case 'h':
					dur *= 60;
				case 'm':
					dur *= 60;
			}

			s = session.optParam("start", null);
			long start = System.currentTimeMillis();
			if (s != null) {
				start = attemptParseDateTime(s).toEpochSecond() * 1000;
			} else
				dur *= -1;

			int count = -1;
			s = session.optParam("count", null);
			if (s != null)
				count = Integer.parseInt(s);

			Format format = Format.valueOf(session.optParam("format", "CSV").toUpperCase());

			List<String> fields = null;
			s = session.optParam("fields", null);
			if (s != null) {
				if (!s.equalsIgnoreCase("all")) {
					fields = new ArrayList<>();
					for (String x : s.split(","))
						fields.add(x.trim());
				}
			}

			long end = start + dur * 1000;
			if (end < start) {
				long t = end;
				end = start;
				start = t;
			}

			DataModel model = owner.getEnv().getModel();
			if (fields == null)
				fields = model.getFields().stream().map(ModelField::getId).collect(Collectors.toList());
			fields.remove("TIMESTAMP"); // we always add this already

			DataLogger data = null;
			for (InvModule m : owner.getEnv().getModules())
				if (m instanceof DataLogger)
					data = (DataLogger) m;
			if (data == null)
				throw new InvMonClientError("Cannot find a DataLogger module");
			DataTable dt;
			if (count != -1) {
				QueryResults res = data.query(start, end, count, fields);
				dt = res.toDataTable();
			} else {
				dt = new DataTable();
				dt.addField("TIMESTAMP");
				for (String f : fields)
					dt.addField(f);
				ModelField[] flds = new ModelField[fields.size() + 1];
				int i = 1;
				flds[0] = model.get("TIMESTAMP");
				for (String f : fields)
					flds[i++] = model.get(f);
				System.out.println("from/to: " + start + "  -  " + end);
				data.forEachRecordIn(start, end, r -> {
					DataRecord rec = dt.addRecord();
					rec.setValue(0, Long.toString(r.getTimestamp()));
					for (int j = 1; j < flds.length; ++j)
						rec.setValue(j, r.getObject(flds[j]));
				});
			}

			switch (format) {
				case CSV:
					CSVExporter csv = new CSVExporter();
					csv.setHeader(true);
					try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
						csv.export(dt, baos);
						return new InvMonHTTPResponse(Status.OK, "text/plain", new String(baos.toByteArray(), StandardCharsets.UTF_8));
					}
				case FIXED:
					return new InvMonHTTPResponse(Status.OK, "text/plain", dt.toString());
				default:
					throw new UnsupportedOperationException();
			}
		} catch (Exception e) {
			LOGGER.error(e);
			return new InvMonHTTPResponse(Status.INTERNAL_ERROR, "text/plain", e.toString());
		}
	}

	/**
	 * try a variety of datetime formats
	 * 
	 * @param s
	 * @return
	 */
	private static OffsetDateTime attemptParseDateTime(String s) {
		List<Function<String, OffsetDateTime>> things = new ArrayList<>();
		things.add(t -> OffsetDateTime.parse(t));
		things.add(t -> LocalDateTime.parse(t).atZone(ZoneId.systemDefault()).toOffsetDateTime());
		things.add(t -> LocalDate.parse(t).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime());
		DateTimeParseException ee = null;
		for (Function<String, OffsetDateTime> x : things) {
			try {
				return x.apply(s);
			} catch (DateTimeParseException e) {
				ee = e;
			}
		}
		throw ee;
	}

	private InvMonHTTPResponse showHelp() throws NotFoundException {
		return new InvMonHTTPResponse(Status.OK, "text/plain", ClientRes.get("datapageinstructions.txt").toString());
	}

}
