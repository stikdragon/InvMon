package uk.co.stikman.invmon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.stikman.invmon.inverter.DeviceStatus;
import uk.co.stikman.invmon.inverter.PIP8048MAX;
import uk.co.stikman.table.DataTable;

public class InvMon {

	private static boolean terminate = false;

	public static void main(String[] args) {
		try {
			SerialPort port = findPort();
			if (port == null)
				return; // quit
			Thread t = new Thread(() -> monitorLoop(port));
			t.start();

			for (;;) {
				int n = System.in.read();
				if (n == 'q') {
					terminate = true;
					break;
				}
			}
			t.join(1000);
		} catch (Throwable th) {
			th.printStackTrace();
		}

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

	private static void monitorLoop(SerialPort port) {
		ConsoleOutput console = new ConsoleOutput(System.out);
		PIP8048MAX inv = new PIP8048MAX();
		inv.open(port);
		try {
			console.clear();
			for (;;) {
				console.beginFrame();
				console.hideCursor();
				if (terminate)
					return;
				DeviceStatus sts = inv.getStatus();
				console.moveTopLeft();
				console.print("        Battery: ").printFloat(sts.getBatteryV(), 1, "V").print(" (").printFloat(sts.getBatteryCapacity(), 1, "%").print(")").newline();
				float current = sts.getBatteryChargeI();
				boolean charging = true;
				if (sts.getBatteryDischargeI() > current) {
					charging = false;
					current = sts.getBatteryDischargeI();
				}
				if (charging)
					console.print("Battery current: ").printFloat(current, 1, "A").print("  [ ").color(ConsoleOutput.BRIGHT_GREEN).print("CHARGING").reset().print(" ]").spaces(20).newline();
				else
					console.print("Battery current: ").printFloat(current, 1, "A").print("  [ ").color(ConsoleOutput.BRIGHT_RED).print("DISCHARGING").reset().print(" ]").spaces(20).newline();

				float pf = sts.getOutputApparentP() == 0 ? 0.0f : (float) sts.getOutputActiveP() / sts.getOutputApparentP();
				console.print("           Load: ").printFloat(sts.getOutputApparentP(), 0, "W").print(" (active: ").printFloat(sts.getOutputActiveP(), 0, "W").print(" PF: ").printFloat(pf, 2).print(")").spaces(20).newline();

				//
				// solar stuff
				//
				console.print("            PV1: ").printFloat(sts.getPv1P(), 0, "W").print(" - ").printFloat(sts.getPv1V(), 1, "V").print(" @ ").printFloat(sts.getPv1I(), 0, "A").spaces(20).newline();
				console.print("            PV2: ").printFloat(sts.getPv2P(), 0, "W").print(" - ").printFloat(sts.getPv2V(), 1, "V").print(" @ ").printFloat(sts.getPv2I(), 0, "A").spaces(20).newline();
				console.print("       PV Total: ").printFloat(sts.getPv2P() + sts.getPv1P(), 0, "W").spaces(20).newline();

				console.showCursor();
				console.endFrame();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}

		} finally {
			inv.close();
		}
	}

}
