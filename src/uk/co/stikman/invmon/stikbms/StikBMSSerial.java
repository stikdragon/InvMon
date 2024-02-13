package uk.co.stikman.invmon.stikbms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;

public class StikBMSSerial implements StikBMSInterface {

	public class BMSOutputStream extends OutputStream {
		private final OutputStream target;

		public BMSOutputStream(OutputStream target) {
			super();
			this.target = target;
		}

		@Override
		public void write(int b) throws IOException {
			if (b == 0x00) {
				target.write(0x01);
				target.write(0x60);
			} else if (b == 0x01) {
				target.write(0x01);
				target.write(0x61);
			} else {
				target.write(b);
			}
		}

	}

	private DataOutputStream	outputstream;
	private DataInputStream		inputstream;
	private SerialPort			serial;
	private int					baud;

	private static final int	CRC16_POLY	= 0x1021;

	public StikBMSSerial(String port, int baud) {
		this(SerialPort.getCommPort(port), baud);
	}

	public StikBMSSerial(SerialPort port, int baud) {
		this.serial = port;
		this.baud = baud;
	}

	@Override
	public void open() throws IOException {
		serial.setBaudRate(baud);
		serial.setNumStopBits(1);
		serial.setNumDataBits(8);
		serial.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 2000, 0);
		serial.setParity(SerialPort.NO_PARITY);
		serial.clearDTR(); // this is important when running over the onboard USB port since the arduino boards use the DTR line to reboot the device
		serial.openPort();
		outputstream = new DataOutputStream(new BMSOutputStream(serial.getOutputStream()));
		inputstream = new DataInputStream(serial.getInputStream());
	}

	@Override
	public void close() throws IOException {
		serial.closePort();
		serial = null;
		inputstream = null;
		outputstream = null;
	}

	protected byte[] query(String query) throws IOException {
		return query(query.getBytes(StandardCharsets.US_ASCII));
	}

	protected byte[] query(byte[] query) throws IOException {
		testConnected();

		serial.getOutputStream().write(0x00); // write a 0 to start
		outputstream.write(query);
		outputstream.writeShort(calcCrc16(query));
		outputstream.flush();
		serial.getOutputStream().write(0x00); // write a 0 as an end marker
		serial.getOutputStream().flush();

		int len = inputstream.readUnsignedShort();
		short crc = (short) inputstream.readUnsignedShort(); // checksum, todo
		byte[] buf = new byte[len];
		inputstream.readFully(buf);

		short x = calcCrc16(buf);
		if (x != crc)
			throw new IOException(String.format("Checksums did not match (%04x / %04x)", crc, x));

		if (len == 4) {
			if (new String(buf, StandardCharsets.US_ASCII).startsWith("ERR"))
				throw new IOException("Device reports error code: " + formatErrorMessage((char) buf[3]) + " (code: '" + (char) buf[3] + "')");
		}
		return buf;
	}

	private static short calcCrc16(byte[] buf) {
		int crc = 0;

		for (byte b : buf) {
			crc ^= (b & 0xFF) << 8;
			for (int i = 0; i < 8; ++i) {
				if ((crc & 0x8000) != 0) {
					crc = (crc << 1) ^ CRC16_POLY;
				} else {
					crc <<= 1;
				}
			}
		}

		return (short) (crc & 0xFFFF);

	}

	private static String formatErrorMessage(char ch) {
		switch (ch) {
			case 'I':
				return "Unknown Instruction";
			case 'Q':
				return "Invalid control byte";
			case 'S':
				return "Message too short";
			case 'C':
				return "Invalid Checksum";
			default:
				return "Unknown Error";
		}
	}

	private void testConnected() throws IOException {
		if (serial == null || !serial.isOpen())
			throw new IOException("Not connected");

	}

	@Override
	public int queryProtocol() throws IOException {
		String res = new String(query("QP"), StandardCharsets.US_ASCII);
		return Integer.parseInt(res);
	}

	@Override
	public String queryVersion() throws IOException {
		return new String(query("QQ"), StandardCharsets.US_ASCII);
	}

	@Override
	public BMSMetrics queryMetrics() throws IOException {
		try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(query("QV")))) {
			//
			// current first
			//
			int cnt = dis.readUnsignedShort(); // expecting this to be 1, for now
			if (cnt != 1)
				throw new IOException("Invalid response from BMS");
			float current = dis.readFloat();

			//
			// then voltages
			//
			cnt = dis.readUnsignedShort();
			float[] v = new float[cnt];
			for (int i = 0; i < cnt; ++i)
				v[i] = dis.readFloat();

			//
			// then temps
			//
			cnt = dis.readUnsignedShort();
			float[] temps = new float[cnt];
			for (int i = 0; i < cnt; ++i)
				temps[i] = dis.readFloat();

			BMSMetrics res = new BMSMetrics();
			res.setVoltages(v);
			res.setCurrent(current);
			res.setTemperatures(temps);
			return res;
		}
	}

	@Override
	public void resetCalib() throws IOException {
		checkResultOK(query("C0"));
	}

	@Override
	public void setCalibFactor(CalibTarget target, int idx, float val) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeByte('S');
		dos.writeByte('C');
		switch (target) {
			case CURRENT:
				dos.writeByte('A');
				break;
			case OFFSET:
				dos.writeByte('O');
				break;
			case VOLTAGE:
				dos.writeByte('V');
				break;
			case VOLTAGE_RAW:
				dos.writeByte('R');
				break;
			default:
				throw new IllegalStateException();

		}
		dos.writeByte(idx);
		dos.writeFloat(val);
		dos.flush();
		checkResultOK(query(baos.toByteArray()));
	}

	private void checkResultOK(byte[] res) throws IOException {
		if (res.length != 2)
			throw new IOException("Response was not 'OK'");
		if (res[0] == 'O' && res[1] == 'K')
			return;
		throw new IOException("Response was not 'OK'");
	}

	@Override
	public List<CalibFactor> getCalibFactors() throws IOException {
		List<CalibFactor> lst = new ArrayList<>();
		byte[] res = query("QC");
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(res));
		int cnt = dis.readShort();
		if (cnt != 1)
			throw new IOException("Expected only one current shunt");
		float f = dis.readFloat();
		lst.add(new CalibFactor("i0", f));

		cnt = dis.readShort();
		for (int i = 0; i < cnt; ++i) {
			f = dis.readFloat();
			lst.add(new CalibFactor("v" + i, f));
		}

		f = dis.readFloat();
		lst.add(new CalibFactor("offset", f));

		return lst;
	}

}
