package uk.co.stikman.invmon.remote;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.DataPoint;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.Sample;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.datalog.MiniDbException;
import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.log.StikLog;

public class JSONRecv extends InvModule {
	private interface Handler {
		JSONObject handle(JSONObject args) throws InvUserError;
	}

	private static final StikLog		LOGGER			= StikLog.getLogger(JSONRecv.class);

	private static final String			ENCRYPT_HELPER	= "encrypt-key";

	private int							port;
	private Map<String, Handler>		handlers		= new HashMap<>();
	private Map<String, EncryptHelper>	encryptKeys		= new HashMap<>();

	private SimpleServer				svr;

	private int							lastRecvId		= 0;

	public JSONRecv(String id, Env env) {
		super(id, env);
		handlers.put("getLastRecvId", this::handleGetLastRecvId);
		handlers.put("sendRecords", this::handleSendRecords);
	}

	@Override
	public void configure(Element config) throws InvMonException {
		this.port = Integer.parseInt(InvUtil.getAttrib(config, "port"));
		for (Element el : InvUtil.getElements(InvUtil.getElement(config, "Keys"))) {
			if (el.getTagName().equals("Key")) {
				String usr = InvUtil.getAttrib(el, "user");
				if (usr.length() != 8)
					throw new InvMonException("Illegal user name for key: " + usr + ". Must be 8 chars");
				encryptKeys.put(usr, new EncryptHelper(InvUtil.getAttrib(el, "key")));
			}
		}
	}

	@Override
	public void start() throws InvMonException {
		LOGGER.info("Starting on port " + port);
		super.start();

		//
		// work out what the last record ID we got was by seeing if there's a 
		// datalogger present and asking that 
		//
		DataLogger logger = getEnv().findModule("datalogger");
		if (logger != null) {
			try {
				lastRecvId = logger.getLastRecordId();
			} catch (MiniDbException e) {
				throw new InvMonException("Failed to start JSONReceiver: " + e.getMessage(), e);
			}
		}

		svr = new SimpleServer(port) {
			@Override
			protected byte[] handleRequest(Request req) {
				return handle(req);
			}
		};
	}

	protected byte[] handle(Request req) {
		try {
			try {
				//
				// first 2 letters are an instruction, the rest of the data is base64 encoded payload
				//
				if (req.getData().length < 2)
					throw new InvUserError("Invalid request");
				String bit = new String(req.getData(), 0, 2, StandardCharsets.ISO_8859_1);
				if ("SK".equals(bit)) {
					//
					// set key
					//
					String k = new String(req.getData(), 2, req.getData().length - 2, StandardCharsets.ISO_8859_1);
					EncryptHelper enc = encryptKeys.get(k);
					if (enc == null)
						throw new InvUserError("Invalid SK");
					req.getSession().put(ENCRYPT_HELPER, enc);
					return "AOK".getBytes(StandardCharsets.ISO_8859_1);

				} else if ("RQ".equals(bit)) {
					//
					// decrypt payload
					//
					EncryptHelper enc = req.getSession().get(ENCRYPT_HELPER);
					if (enc == null)
						throw new InvUserError("SK has not been called");

					JSONObject jo = new JSONObject(enc.decryptUTF8(req.getData(), 2, req.getData().length - 2));
					String s = jo.getString("func");

					Handler h = handlers.get(s);
					if (h == null)
						throw new InvUserError("Invalid request");
					JSONObject r = h.handle(jo);
					return enc.encryptUTF8("A" + r.toString());
				} else
					throw new InvUserError("Unknown instruction: " + bit);

			} catch (JSONException je) {
				throw new InvUserError(je.getMessage(), je);
			}
		} catch (InvUserError ue) {
			//
			// These types of exception are safe to send back, but not the stack trace.
			// Errors are never encrypted
			//
			LOGGER.error(ue);
			return ("E" + ue.getMessage()).getBytes(StandardCharsets.ISO_8859_1);
		} catch (Exception e) {
			LOGGER.error(e);
			return ("EInternal Error").getBytes(StandardCharsets.ISO_8859_1);
		}
	}

	@Override
	public void terminate() {
		LOGGER.info("Terminating");
		if (svr != null)
			svr.stop();
		svr = null;
		LOGGER.info("  done");
		super.terminate();
	}

	private JSONObject handleGetLastRecvId(JSONObject data) {
		return new JSONObject().put("lastId", lastRecvId);
	}

	private JSONObject handleSendRecords(JSONObject data) throws InvUserError {
		JSONArray arr = data.getJSONArray("records");
		DataModel model = getEnv().getModel();
		for (int i = 0; i < arr.length(); ++i) {
			JSONObject jo = arr.getJSONObject(i);
			int recid = jo.getInt("recId");

			//
			// we're pretending to be an inverter, so we post this data to our own bus
			//
			PollData pd = new PollData();
			Sample dp = new Sample(jo.getLong("ts"));
			JSONArray arr2 = jo.getJSONArray("data");
			for (int j = 0; j < arr2.length(); ++j) {
				JSONObject jo2 = arr2.getJSONObject(j);
				Field f = model.get(jo2.getString("f"));
				switch (f.getType().getBaseType()) {
					case FLOAT:
						dp.put(f.getId(), jo2.getFloat("v"));
						break;
					case INT:
						dp.put(f.getId(), jo2.getInt("v"));
						break;
					case STRING:
						dp.put(f.getId(), jo2.getString("v"));
						break;
					default:
						throw new InvUserError("Unsupported field: " + f.getId());
				}
			}
			pd.add(jo.getString("src"), dp);
			getEnv().getBus().fire(Events.POST_DATA, pd);
			lastRecvId = recid;
		}
		return new JSONObject(); // nothing to reply
	}
}
