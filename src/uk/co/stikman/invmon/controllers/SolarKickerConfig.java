package uk.co.stikman.invmon.controllers;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.inverter.util.PhysicalQuantity;

public class SolarKickerConfig {
	public class Channel {
		private final int	id;
		private float		threshV		= -1f;
		private float		threshI		= -1f;
		private int			threshTime	= -1;
		private String		source;
		private String		action;

		private float		curV;
		private float		curI;
		private long		threshMetAt;

		public Channel(int id) {
			this.id = id;
		}

		public void setCurV(float curV) {
			this.curV = curV;
		}

		public void setCurI(float curI) {
			this.curI = curI;
		}

		public void setThreshMetAt(long threshMetAt) {
			this.threshMetAt = threshMetAt;
		}

		public float getThreshV() {
			return threshV;
		}

		public float getThreshI() {
			return threshI;
		}

		public int getThreshTime() {
			return threshTime;
		}

		public String getSource() {
			return source;
		}

		public String getAction() {
			return action;
		}

		public float getCurV() {
			return curV;
		}

		public float getCurI() {
			return curI;
		}

		public long getThreshMetAt() {
			return threshMetAt;
		}

		public boolean satisfiesThreshold(float v, float i) {
			return i >= threshI && v <= threshV;
		}

		@Override
		public String toString() {
			return source + " -> " + action;
		}

	}

	String					portName;
	private List<Channel>	channels	= new ArrayList<>();
	private long			contactorTime;

	public void parseConfig(Element root) throws InvMonException {
		portName = InvUtil.getAttrib(root, "port");

		String s = InvUtil.getAttrib(root, "contactorTime", "1000ms");
		contactorTime = InvUtil.parseMilliseconds(s);

		int idx = 1;
		for (Element el : InvUtil.getElements(root, "Channel")) {
			Channel ch = new Channel(idx++);
			s = InvUtil.getAttrib(el, "condition");
			String[] bits = s.split(",");
			for (String t : bits) {
				t = t.replaceAll("\\s", "").toLowerCase(); // remove whitespace
				if (t.startsWith("v:")) {
					ch.threshV = InvUtil.parsePhysical(PhysicalQuantity.VOLTAGE, t.substring(2));
				} else if (t.startsWith("i:")) {
					ch.threshI = InvUtil.parsePhysical(PhysicalQuantity.CURRENT, t.substring(2));
				} else if (t.startsWith("t:")) {
					ch.threshTime = (int) InvUtil.parseMilliseconds(t.substring(2));
				} else
					throw new InvMonException("Invalid condition attribute: " + s);
			}

			s = InvUtil.getAttrib(el, "source").trim();
			if (s.isEmpty())
				throw new InvMonException("SolarKicker->Channel source attrib is empty");
			s = s.replaceAll("\\s", ""); // remove whitespace
			if (!s.matches("^[a-zA-Z0-9_]+\\.[a-zA-Z0-9]+$"))
				throw new InvMonException("Expected inverter.field for SolarKicker source attribute: " + s);
			ch.source = s;
			ch.threshMetAt = System.currentTimeMillis();

			ch.action = InvUtil.getAttrib(el, "action").toLowerCase().trim();
			if (!ch.action.equals("dtr") && !ch.action.equals("rts"))
				throw new InvMonException("action attribute for SolarKicker is not supported: " + ch.action);

			channels.add(ch);
		}
	}

	public String getPortName() {
		return portName;
	}

	public List<Channel> getChannels() {
		return channels;
	}

	public long getContactorTime() {
		return contactorTime;
	}

}