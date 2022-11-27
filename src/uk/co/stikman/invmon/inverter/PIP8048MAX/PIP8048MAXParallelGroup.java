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

public class PIP8048MAXParallelGroup extends InvModule {

	private final List<InverterMonitor>		children	= new ArrayList<>();
	private final List<InvModDefinition>	childDefs	= new ArrayList<>();

	private Field							fieldMode;
	private Field							fieldChargeState;
	private FieldVIF						fieldLoad;
	private FieldVIF						fieldPv1;
	private FieldVIF						fieldPv2;
	private Field							fieldTemperature;
	private Field							fieldBusVoltage;
	private Field							fieldLoadPF;
	private Field							fieldStateOfCharge;
	private Field							fieldMisc;
	private Field							fieldPv1P;
	private Field							fieldPv2P;
	private Field							fieldBattV;
	private Field							fieldBattI1;
	private Field							fieldBattI2;

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

		if (childDefs.size() != 2)
			throw new InvMonException("Not supported: Currently only capable of tracking two inverters");
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
		DataPoint second = in.get(1);

		//
		// fields that we get frmo the first one
		//
		out.put(fieldMode, first.getString(fieldMode));
		out.put(fieldChargeState, first.getString(fieldChargeState));
		out.put(fieldStateOfCharge, first.getFloat(fieldStateOfCharge));

		//
		// these need aggregating in some way
		//
//		out.put(fieldBattV, first.getFloat(fieldBattI1) sts.getBatteryV(), charging ? sts.getBatteryChargeI() : -sts.getBatteryDischargeI(), 0);
//		float maxp = Math.max(sts.getOutputActiveP(), sts.getOutputApparentP());
//		out.put(fieldLoad, sts.getOutputV(), maxp / sts.getOutputV(), sts.getOutputF());
//
//		//
//		// these are separate for each inverter
//		//
//		out.put(fieldBattI1, maxp)
//		out.put(fieldTemperature, sts.getInverterTemp());
//		out.put(fieldBusVoltage, sts.getBusV());
//		out.put(fieldLoadPF, pf);
//		out.put(fieldMisc, sts.getDeviceStatus() + " / " + sts.getDeviceStatus2());
//
//		out.put(fieldPv1, sts.getPv1V(), sts.getPv1I(), 0);
//		out.put(fieldPv2, sts.getPv2V(), sts.getPv2I(), 0);
//		out.put(fieldPv1P, sts.getPv1V() * sts.getPv1I());
//		out.put(fieldPv2P, sts.getPv2V() * sts.getPv2I());

		//
		// so they should all have the same set of fields, we can group them
		// together
		// oooooooh this is a mess
		//
//		DataPoint first = in.get(0);
		for (Entry<Field, Object> e : first.getValues().entrySet())
			out.put(e.getKey(), e.getValue());
		for (DataPoint dp : in) {
			if (dp == first)
				continue;

			for (Entry<Field, Object> e : first.getValues().entrySet()) {
				Object val = out.getValues().get(e.getKey());
				switch (e.getKey().getType()) {
					case CURRENT:
					case POWER:
						out.getValues().put(e.getKey(), ((Number) val).floatValue() + ((Number) e.getValue()).floatValue());
						break;
					case VOLTAGE: // max
						out.getValues().put(e.getKey(), Math.max(((Number) val).floatValue(), ((Number) e.getValue()).floatValue()));
						break;
					case FREQ: // first (ie. do nothing)
					case STRING:
					case TIMESTAMP:
						break;

					case FLOAT: // check the aggregation method
						if (e.getKey().getAggregationMode() != AggregationMode.MEAN)
							throw new RuntimeException("I have not implemented this yet");
						out.getValues().put(e.getKey(), ((Number) val).floatValue() + ((Number) e.getValue()).floatValue());
						break;
					case INT:
						if (e.getKey().getAggregationMode() != AggregationMode.MEAN)
							throw new RuntimeException("I have not implemented this yet");
						out.getValues().put(e.getKey(), ((Number) val).intValue() + ((Number) e.getValue()).intValue());
						break;
					default:
						throw new RuntimeException("I have not implemented this yet");
				}
			}
		}

		//
		// now sort out averages
		//
		int N = in.size();
		for (Entry<Field, Object> e : first.getValues().entrySet()) {
			Object val = out.getValues().get(e.getKey());
			switch (e.getKey().getType()) {
				case CURRENT:
				case POWER:
				case FLOAT:
					out.getValues().put(e.getKey(), ((Number) val).floatValue() / N);
					break;
				case INT:
					out.getValues().put(e.getKey(), ((Number) val).intValue() / N);
					break;
				case VOLTAGE: // max (do nothing)
				case FREQ: // first (do nothing)
				case STRING:
				case TIMESTAMP:
					break;
				default:
					throw new RuntimeException("I have not implemented this yet");
			}
		}

		data.add(getId(), out);
	}

	@Override
	public void start() throws InvMonException {
		super.start();

		DataModel model = getEnv().getModel();
		fieldMode = model.get("INV_MODE");
		fieldChargeState = model.get("BATT_MODE");
		fieldBattV = model.get("BATT_V");
		fieldBattI1 = model.get("BATT_I1");
		fieldBattI2 = model.get("BATT_I2");
		fieldLoad = model.getVIF("LOAD");
		fieldPv1 = model.getVIF("PV1");
		fieldPv2 = model.getVIF("PV2");
		fieldPv1P = model.get("PV1_P");
		fieldPv2P = model.get("PV2_P");
		fieldTemperature = model.get("INV_TEMP");
		fieldBusVoltage = model.get("INV_BUS_V");
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
