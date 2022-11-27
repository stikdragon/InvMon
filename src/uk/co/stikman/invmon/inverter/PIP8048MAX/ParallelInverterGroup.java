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
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.inverter.InvUtil;

public class ParallelInverterGroup extends InvModule {

	private final List<InverterMonitor>		children	= new ArrayList<>();
	private final List<InvModDefinition>	childDefs	= new ArrayList<>();

	public ParallelInverterGroup(String id, Env env) {
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

		//
		// so they should all have the same set of fields, we can group them
		// together
		// oooooooh this is a mess
		//
		DataPoint first = in.get(0);
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
