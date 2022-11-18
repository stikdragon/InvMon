package uk.co.stikman.invmon.junk;

import java.io.IOException;
import java.io.InputStream;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.log.StikLog;

public class ArduinoThing {
	private static final StikLog LOGGER = StikLog.getLogger(ArduinoThing.class);

	public static void main(String[] args) throws Exception {
		new ArduinoThing().go(args[0]);
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
		port.setBaudRate(1200);
		port.setNumStopBits(1);
		port.setNumDataBits(8);
		port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 100, 0);
		port.setParity(SerialPort.NO_PARITY);
		port.openPort();
		try {

			InputStream is = port.getInputStream();

			for (;;) {

				int n = is.read();
				System.out.println(String.format("0x%2x\n", n));

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				if (terminate)
					return;
			}
		} catch (IOException e) {
			LOGGER.error("Port error: " + e.getMessage(), e);
		} finally {
			port.closePort();
		}

	}
}
