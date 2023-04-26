package uk.co.stikman.invmon;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.log.StikLog;

public class SerialRepeater extends InvModule {
	private static final StikLog	LOGGER	= StikLog.getLogger(SerialRepeater.class);
	private int						baud;

	private List<OutputField>		fields	= new ArrayList<>();
	private Output					output;

	private static class OutputField {
		Field	f;
		int		width;
		float	scale;
	}

	public interface Output {
		void config(Element config) throws InvMonException;

		void start();

		void terminate();

		void send(String msg);
	}

	public static class OutputPort implements Output {
		private int			baud;
		private SerialPort	port;

		@Override
		public void config(Element config) throws InvMonException {
			baud = Integer.parseInt(InvUtil.getAttrib(config, "baud", "9600"));
			String portName = InvUtil.getAttrib(config, "port");
			this.port = null;
			for (SerialPort p : SerialPort.getCommPorts()) {
				if (p.getSystemPortName().equals(portName))
					this.port = p;
			}
			if (port == null)
				throw new InvMonException("Cannot find port called [" + portName + "]");

		}

		@Override
		public void start() {
			//
			// open serial port
			//
			port.setBaudRate(baud);
			port.openPort();
		}

		@Override
		public void terminate() {
			//
			// close serial port
			//
			try {
				port.closePort();
			} catch (Exception e) {
				LOGGER.error("Failed to close serial port");
			}
		}

		@Override
		public void send(String msg) {
			//
			// turn that into a series of bytes in ASCII format, calculate a very simple
			// checksum, and send it out over serial
			//
			byte[] bytes = msg.getBytes(StandardCharsets.US_ASCII);
			int checksum = 0;
			for (byte b : bytes)
				checksum += b;

			try (OutputStream os = port.getOutputStream()) {
				os.write(bytes, 0, bytes.length);
				os.write((checksum >> 8) & 0xff);
				os.write(checksum & 0xff);
			} catch (IOException e) {
				LOGGER.error("Failed to write to serial port because: " + e.getMessage(), e);
			}
		}

	}

	public static class OutputConsole implements Output {
		@Override
		public void config(Element config) throws InvMonException {
		}

		@Override
		public void start() {
		}

		@Override
		public void terminate() {
		}

		@Override
		public void send(String msg) {
			System.out.println(msg);
		}

	}

	public SerialRepeater(String id, Env env) {
		super(id, env);
		output = new OutputPort();
//		output = new OutputConsole();
	}

	@Override
	public void configure(Element config) throws InvMonException {
		for (Element el : InvUtil.getElements(config, "Field")) {
			OutputField x = new OutputField();
			x.f = getEnv().getModel().get(el.getTextContent());
			x.width = Integer.parseInt(InvUtil.getAttrib(el, "width"));
			x.scale = Float.parseFloat(InvUtil.getAttrib(el, "scale"));
			fields.add(x);
		}
		output.config(config);
	}

	@Override
	public void start() throws InvMonException {
		super.start();
		output.start();
	}

	@Override
	public void terminate() {
		output.terminate();
		super.terminate();
	}

	@Subscribe(Events.LOGGER_RECORD_COMMITED)
	public void postData(DBRecord rec) {
		//
		// make a message with the things we're interested in, terminated by a \n
		//
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (OutputField of : fields) {
			float a = rec.getFloat(of.f);
			int n = (int) (a * of.scale);
			sb.append(sep);
			sep = ",";
			sb.append(String.format("%0" + of.width + "d", n));
		}
		sb.append("\n");
		output.send(sb.toString());
	}

}
