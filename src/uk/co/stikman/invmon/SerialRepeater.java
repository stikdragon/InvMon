package uk.co.stikman.invmon;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.w3c.dom.Element;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.log.StikLog;

public class SerialRepeater extends InvModule {
	private static final StikLog	LOGGER	= StikLog.getLogger(SerialRepeater.class);
	private SerialPort				port;

	private Field					fSOC;
	private Field					fVoltage;

	public SerialRepeater(String id, Env env) {
		super(id, env);
	}

	@Override
	public void configure(Element config) throws InvMonException {
		String portName = InvUtil.getAttrib(config, "port");
		this.port = null;
		for (SerialPort p : SerialPort.getCommPorts()) {
			if (p.getSystemPortName().equals(portName))
				this.port = p;
		}
	}

	@Override
	public void start() throws InvMonException {
		super.start();
		fSOC = getEnv().getModel().get("BATT_SOC");
		fVoltage = getEnv().getModel().get("BATT_V");

		//
		// open serial port
		//
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
		super.terminate();
	}

	@Subscribe(Events.LOGGER_RECORD_COMMITED)
	public void postData(DBRecord rec) {
		//
		// make a message with the things we're interested in, terminated by a \n
		//
		float soc = rec.getFloat(fSOC);
		float battv = rec.getFloat(fVoltage);
		String msg = String.format("%d %.2f\n", (int) (soc * 100.0f), battv);
		
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
