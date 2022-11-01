package uk.co.stikman.invmon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.stikman.table.DataTable;

public class InvMon {
	public static void main(String[] args) throws Exception {
		Env env = new Env();
		env.start();

		for (;;) {
			int n = System.in.read();
			if (n == 'q') {
				env.terminate();
				break;
			}
		}

		env.awaitTermination();
	}

	private static SerialPort findPort() throws IOException {
		//
		// print a list of serial ports in a nice table
		//
		System.out.println("Available serial ports:");
		DataTable dt = new DataTable();
		dt.addFields("Num", "Port", "Path", "Description");
		int i = 0;
		SerialPort[] ports = SerialPort.getCommPorts();
		for (SerialPort x : ports)
			dt.addRecord(Integer.toString(++i), x.getSystemPortName(), x.getSystemPortPath(), x.getDescriptivePortName());
		System.out.println(dt.toString());

		//
		// ask them which one they want to use
		//
		BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
		for (;;) {
			System.out.println("Type ID of port to use, or 'q' to quit:");
			try {
				String s = r.readLine();
				if ("q".equalsIgnoreCase(s))
					return null;
				int id = Integer.parseInt(s) - 1;
				return ports[id];
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}
	}

}
