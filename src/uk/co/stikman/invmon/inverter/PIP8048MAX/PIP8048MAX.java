package uk.co.stikman.invmon.inverter.PIP8048MAX;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.stikman.invmon.inverter.CommunicationError;
import uk.co.stikman.invmon.inverter.Template;
import uk.co.stikman.invmon.inverter.TemplateResult;

public class PIP8048MAX {

	private SerialPort			port;
	private long				started;
	private Consumer<String>	logHandler;


	public Consumer<String> getLogHandler() {
		return logHandler;
	}

	public void setLogHandler(Consumer<String> logHandler) {
		this.logHandler = logHandler;
	}

	public PIP8048MAX() {
		started = System.currentTimeMillis();
	}

	public void open(SerialPort port) {
		if (isOpen())
			throw new IllegalStateException("Port already open");
		this.port = port;
		port.setBaudRate(2400);
		port.setNumStopBits(1);
		port.setNumDataBits(8);
		port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 100, 0);
		port.setParity(SerialPort.NO_PARITY);
		port.openPort();

		//
		// check the protocol id here 
		//
		String s = query("QPI");
		if (!s.equals("PI30"))
			throw new CommunicationError("Expected protocol PI30, inverter is: " + s);
	}

	public void close() {
		if (!isOpen())
			throw new IllegalStateException("Port is not open");
		port.closePort();
		port = null;
	}

	private boolean isOpen() {
		return port != null;
	}

	private void checkOpen() {
		if (!isOpen())
			throw new CommunicationError("Port is not connected");
	}

	private String query(String command) {
		checkOpen();
		try {
			send(command);
			return recv();
		} catch (IOException e) {
			throw new CommunicationError("Query failed: " + e.getMessage(), e);
		}
	}

	private String recv() throws IOException {
		try (InputStream is = port.getInputStream()) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			//
			// read until <cr>
			//
			for (;;) {
				int n = is.read();
				if (n == -1)
					throw new IOException("Stream EOF");
				if (n == 0x0d)
					break;
				baos.write(n);
			}

			//
			// so all responses start with a ( char, then the data, then a CRC, then 
			// the <cr> we've already stripped off 
			//
			byte[] buf = baos.toByteArray();
			log("recv: " + formatHex(buf));

			if (buf.length < 4)
				throw new CommunicationError("Invalid response: (too short)");
			if (buf[0] != '(')
				throw new CommunicationError("Invalid response: (incorrect first byte)");
			int crc = ((buf[buf.length - 2] & 0xff) << 8) + (buf[buf.length - 1] & 0xff);
			String s = new String(buf, 1, buf.length - 3, StandardCharsets.US_ASCII);
			log("recv: (" + s + ")");
			if (calcCrc("(" + s) != crc)
				throw new CommunicationError("Corrupt response, checksum did not match");
			return s;
		}
	}

	private void send(String data) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write(data.getBytes(StandardCharsets.US_ASCII));
		int crc = calcCrc(data); // it seems to ignore this anyhow, which is concerning :s
		os.write(crc >> 8 & 0xff);
		os.write(crc & 0xff);
		os.write(0x0d); // <cr>
		os.flush();

		log("send: " + formatHex(os.toByteArray()));

		port.writeBytes(os.toByteArray(), os.size());
	}

	private void log(String s) {
		long dt = System.currentTimeMillis() - started;
		if (logHandler != null)
			logHandler.accept("[" + dt + "] " + s);
	}

	private String formatHex(byte[] buf) {
		StringBuilder sb = new StringBuilder();
		for (byte b : buf) {
			int n = b & 0xff;
			if (n <= 0xf)
				sb.append("0");
			sb.append(Integer.toHexString(n).toUpperCase());
			sb.append(" ");
		}
		return sb.toString();
	}

	private static final int[] CRC_TABLE = new int[] { 0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50a5, 0x60c6, 0x70e7, 0x8108, 0x9129, 0xa14a, 0xb16b, 0xc18c, 0xd1ad, 0xe1ce, 0xf1ef };

	/**
	 * this seems to be a weird type of CRC16 with a custom table perhaps? and
	 * some fettling at the end
	 * 
	 * @param data
	 * @return
	 */
	public static int calcCrc(String data) {
		int crc = 0;
		int ptr = 0;
		int da;
		int hi;
		int lo;

		int len = data.length();
		while (len-- != 0) {
			int v = data.charAt(ptr++);
			da = ((crc >> 8) & 0xff) >> 4;
			crc <<= 4;
			crc ^= CRC_TABLE[da ^ (v >> 4)];
			da = ((crc >> 8) & 0xff) >> 4;
			crc <<= 4;
			crc ^= CRC_TABLE[da ^ (v & 0xf)];
		}

		lo = crc & 0xff;
		hi = (crc >> 8) & 0xff;

		//
		// looks like the protocol uses \r, \n and ( as special characters 
		// so the checksum is adjusted to never contain those bytes
		//
		if (lo == 0x28 || lo == 0x0d || lo == 0x0a)
			lo++;
		if (hi == 0x28 || hi == 0x0d || hi == 0x0a)
			hi++;
		return (hi << 8) + lo;
	}

	public String getDeviceSerial() {
		String s = query("QSID");
		//
		// first two are the length of serial number
		//
		int len = Integer.parseInt(s.substring(0, 2));
		if (len > 20)
			throw new CommunicationError("Unexpected length: " + len);
		return s.substring(2, len + 2);
	}

	private static final Template TPL_QVFW = new Template("VERFW:<NNNNN.NN>");

	public String getFirmwareVersion() {
		TemplateResult parts = TPL_QVFW.apply(query("QVFW"));
		return parts.getString("N");
	}

	private static final Template TPL_QVFW3 = new Template("VERFW:<NNNNN.NN>");

	public String getRemotePanelFirmwareVersion() {
		TemplateResult parts = TPL_QVFW3.apply(query("QVFW3"));
		return parts.getString("N");
	}

	private static final Template TPL_QPIRI = new Template("<BBB.B> <CC.C> <DDD.D> <EE.E> <FF.F> <HHHH> <IIII> <JJ.J> <KK.K> <LL.L> <MM.M> <NN.N> <O> <PPP> <QQQ> <R> <S> <T> <r> <ss> <t> <U> <VV.V> <W> <X> <YYY>");

	public DeviceRatingInfo getDeviceRatingInfo() {
		TemplateResult parts = TPL_QPIRI.apply(query("QPIRI"));
		DeviceRatingInfo dri = new DeviceRatingInfo();
		dri.fromTemplateResult(parts);
		return dri;
	}

	public DeviceFlags getDeviceFlags() {
		DeviceFlags flags = new DeviceFlags();
		flags.fromLine(query("QFLAG"));
		return flags;
	}

	private static final Template	TPL_QPIGS	= new Template("<BBB.B> <CC.C> <DDD.D> <EE.E> <FFFF> <GGGG> <HHH> <III> <JJ.JJ> <KKK> <OOO> <TTTT> <ee.e> <UUU.U> <WW.WW> <PPPPP> <AAAAAAAA> <QQ> <VV> <MMMMM> <ZZZ>");
	private static final Template	TPL_QPIGS2	= new Template("<AA.A> <BBB.B> <CCCCC>");

	public DeviceStatus getStatus() {
		DeviceStatus s = new DeviceStatus();
		s.fromTemplateResultPart1(TPL_QPIGS.apply(query("QPIGS")));
		s.fromTemplateResultPart2(TPL_QPIGS2.apply(query("QPIGS2")));
		return s;
	}

	public DeviceMode getDeviceMode() {
		String s = query("QMOD");
		if (s.length() != 2)
			throw new CommunicationError("Invalid response: " + s);
		if (s.charAt(0) != '(')
			throw new CommunicationError("Invalid response: " + s);
		switch (s.charAt(1)) {
		case 'P':
			return DeviceMode.POWER_ON;
		case 'S':
			return DeviceMode.STANDBY;
		case 'L':
			return DeviceMode.LINE;
		case 'B':
			return DeviceMode.BATTERY;
		case 'F':
			return DeviceMode.FAULT;
		case 'D':
			return DeviceMode.SHUTDOWN;
		}
		throw new CommunicationError("Invalid response: " + s);
	}

}
