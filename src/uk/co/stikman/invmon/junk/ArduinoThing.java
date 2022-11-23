package uk.co.stikman.invmon.junk;

import java.io.IOException;
import java.io.InputStream;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortTimeoutException;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.log.StikLog;

public class ArduinoThing {
	private static final StikLog LOGGER = StikLog.getLogger(ArduinoThing.class);

	public static void main(String[] args) throws Exception {
		String p;
		if (args.length == 0)
			p = "COM8";
		else
			p = args[0];

		new ArduinoThing().go(p);
	}

	private SerialPort	port;
	private boolean		terminate;

	private void go(String name) throws InterruptedException, IOException {
		Env.listPorts();
		this.port = null;
		for (SerialPort p : SerialPort.getCommPorts()) {
			if (p.getSystemPortName().equals(name))
				this.port = p;
		}
		if (port == null)
			throw new RuntimeException("Port not found");

		Thread thrd = new Thread(() -> doIt(port));
		thrd.start();

		for (;;) {
			int ch = System.in.read();
			if (ch == 'q') {
				terminate = true;
				break;
			}
		}

		thrd.join();
	}

	private void doIt(SerialPort port) {
		port.setBaudRate(2400);
		port.setNumStopBits(1);
		port.setNumDataBits(8);
		port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 0);
		port.setParity(SerialPort.NO_PARITY);
		port.openPort();
		try {
			InputStream is = port.getInputStream();

			for (;;) {
				if (terminate)
					return;
				int n;
				try {
					n = is.read();
				} catch (SerialPortTimeoutException e) {
					continue;
				}
				System.out.println(String.format("0x%2x ", n));

				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		} catch (IOException e) {
			LOGGER.error("Port error: " + e.getMessage(), e);
		} finally {
			port.closePort();
		}

	}
}
