package uk.co.stikman.invmon;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.stikman.table.DataTable;

public class InvMon {

	public static void main(String[] args) throws Exception {

		listPorts();

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

	private static void listPorts() {
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

	}

}
