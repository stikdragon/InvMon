package uk.co.stikman.invmon.inverter.PIP8048MAX;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.Config;
import uk.co.stikman.invmon.DataPoint;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.InvModDefinition;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.InverterMonitor;
import uk.co.stikman.invmon.PollData;
import uk.co.stikman.invmon.datamodel.AggregationMode;
import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.datamodel.FieldVIF;
import uk.co.stikman.invmon.datamodel.InverterMode;
import uk.co.stikman.invmon.inverter.InvUtil;
import uk.co.stikman.table.DataField;

public class PIP8048MAXParallelGroup extends InvModule {

	private final List<InverterMonitor>		children	= new ArrayList<>();
	private final List<InvModDefinition>	childDefs	= new ArrayList<>();

	private Field							fieldMode;
	private Field							fieldChargeState;
	private FieldVIF[]						fieldPvA;
	private FieldVIF[]						fieldPvB;
	private Field[]							fieldTemperature;
	private Field[]							fieldBusVoltage;
	private Field							fieldLoadPF;
	private Field							fieldStateOfCharge;
	private Field							fieldMisc;
	private Field[]							fieldPvAP;
	private Field[]							fieldPvBP;
	private Field							fieldBattV;
	private Field[]							fieldBattI;
	private Field							fieldLoadV;
	private Field[]							fieldLoadI;

	public PIP8048MAXParallelGroup(String id, Env env) {
		super(id, env);
	}

	@Override
	public void configure(Element config) throws InvMonException {
		//
		// look for inverters within this
		//
		for (Element el : InvUtil.getElements(config)) {
			Class<? extends InvModule> cls = Config.getThingTypes().get(el.getTagName());
			if (cls == null)
				throw new InvMonException("Unknown inverter type: " + el.getTagName());
			if (!InverterMonitor.class.isAssignableFrom(cls))
				throw new InvMonException("Unknown inverter type: " + el.getTagName());
			InvModDefinition def = new InvModDefinition(InvUtil.getAttrib(el, "id"), cls, el);
			childDefs.add(def);
		}

		if (childDefs.size() < 2)
			throw new InvMonException("Requires at least one inverter");
	}

	@Subscribe(Events.POLL_SOURCES)
	public void poll(PollData data) {
		//
		// get datapoints from our children and aggregate them
		//
		DataPoint out = new DataPoint(data.getTimestamp());
		List<DataPoint> in = new ArrayList<>();
		for (InverterMonitor x : children)
			in.add(x.createDataPoint(data.getTimestamp()));
		DataPoint first = in.get(0);

		//
		// fields that we get frmo the first one
		//
		out.put(fieldMode, first.getString(fieldMode));
		out.put(fieldChargeState, first.getString(fieldChargeState));
		out.put(fieldStateOfCharge, first.getFloat(fieldStateOfCharge));
		out.put(fieldBattV, first.getFloat(fieldBattV));
		out.put(fieldMisc, first.getString(fieldMisc));
		out.put(fieldLoadV, first.getFloat(fieldLoadV));
		out.put(fieldLoadPF, first.getFloat(fieldLoadPF));

		//
		// these are separate for each inverter
		//
		int idx = 0;
		for (DataPoint dp : in) {
			out.put(fieldBattI[idx], dp.getFloat(fieldBattI[0]));
			out.put(fieldTemperature[idx], dp.getFloat(fieldTemperature[0]));
			out.put(fieldBusVoltage[idx], dp.getFloat(fieldBusVoltage[0]));
			out.put(fieldPvA[idx], dp.getVIF(fieldPvA[0]));
			out.put(fieldPvB[idx], dp.getVIF(fieldPvB[0]));
			out.put(fieldPvAP[idx], dp.getFloat(fieldPvAP[0]));
			out.put(fieldPvBP[idx], dp.getFloat(fieldPvBP[0]));
			out.put(fieldLoadI[idx], dp.getFloat(fieldLoadI[0]));
			++idx;
		}

		data.add(getId(), out);
	}

	@Override
	public void start() throws InvMonException {
		super.start();

		int repeatCount = getEnv().getConfig().getInverterCount();
		DataModel model = getEnv().getModel();
		fieldMode = model.get("INV_MODE");
		fieldChargeState = model.get("BATT_MODE");
		fieldBattV = model.get("BATT_V");

		fieldBattI = new Field[repeatCount];
		fieldTemperature = new Field[repeatCount];
		fieldLoadV = model.get("LOAD_V");
		fieldLoadI = new Field[repeatCount];
		fieldLoadPF = model.get("LOAD_PF");
		fieldPvA = new FieldVIF[repeatCount];
		fieldPvB = new FieldVIF[repeatCount];
		fieldPvAP = new Field[repeatCount];
		fieldPvBP = new Field[repeatCount];
		fieldBusVoltage = new Field[repeatCount];
		for (int i = 0; i < repeatCount; ++i) {
			fieldTemperature[i] = model.get("INV_" + (i + 1) + "_TEMP");
			fieldBattI[i] = model.get("BATT_I_" + (i + 1));
			fieldPvA[i] = model.getVIF("PVA_" + (i + 1));
			fieldPvB[i] = model.getVIF("PVB_" + (i + 1));
			fieldPvAP[i] = model.get("PVA_" + (i + 1) + "_P");
			fieldPvBP[i] = model.get("PVB_" + (i + 1) + "_P");
			fieldBusVoltage[i] = model.get("INV_" + (i + 1) + "_BUS_V");
			fieldLoadI[i] = model.get("LOAD_" + (i + 1) + "_I");
		}
		fieldLoadPF = model.get("LOAD_PF");
		fieldStateOfCharge = model.get("BATT_SOC");
		fieldMisc = model.get("MISC");

		for (InvModDefinition def : childDefs) {
			try {
				InverterMonitor part = (InverterMonitor) def.getClazz().getConstructor(String.class, Env.class).newInstance(def.getId(), this.getEnv());
				part.setGrouped(true);
				part.configure(def.getConfig());
				children.add(part);
			} catch (Exception e) {
				throw new InvMonException("Failed to start module [" + def.getId() + "]: " + e.getMessage(), e);
			}
		}

		for (InvModule m : children)
			m.start();
	}

	@Override
	public void terminate() {
		try {
			for (InvModule m : children)
				m.terminate();
		} finally {
			super.terminate();
		}
	}

}
