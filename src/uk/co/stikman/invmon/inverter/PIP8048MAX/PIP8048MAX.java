package uk.co.stikman.invmon.inverter.PIP8048MAX;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.stikman.invmon.inverter.CommunicationError;
import uk.co.stikman.invmon.inverter.InverterModel;
import uk.co.stikman.invmon.inverter.Template;
import uk.co.stikman.invmon.inverter.TemplateResult;
import uk.co.stikman.log.StikLog;

public class PIP8048MAX implements InverterModel {
	private static final int		MAX_RETRY		= 8;
	private static final StikLog	LOGGER			= StikLog.getLogger(PIP8048MAX.class);
	private SerialPort				port;
	private long					started;
	private boolean					logSerialData	= false;
	private String					lastQPIGS		= "n/a";

	public PIP8048MAX() {
		started = System.currentTimeMillis();
	}

	public void open(SerialPort port) {
		if (isOpen())
			throw new IllegalStateException("Port already open");
		LOGGER.info("Opening port...");
		this.port = port;
		port.setBaudRate(2400);
		port.setNumStopBits(1);
		port.setNumDataBits(8);
		port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 500, 0);
		port.setParity(SerialPort.NO_PARITY);

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
		return port != null && port.isOpen();
	}

	private String query(String command) {
		int failures = 0;
		for (;;) {
			try {
				if (!isOpen()) {
					port.openPort();
					port.flushIOBuffers();
				}
				send(command);
				return recv();

			} catch (Exception ex) {
				++failures;
				if (failures >= MAX_RETRY)
					throw new CommunicationError("Query failed after [" + failures + "] retry attempts: " + ex.getMessage(), ex);
				LOGGER.warn("Query error: " + ex.getMessage(), ex);
				port.closePort();
			}

		}

	}

	private String recv() throws IOException {
		byte[] buf = null;
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
			buf = baos.toByteArray();
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
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			LOGGER.error("Receive Buffer: " + formatHex(buf));
			throw e;
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
		if (logSerialData)
			LOGGER.info(s);
	}

	private static String formatHex(byte[] buf) {
		if (buf == null)
			return "-null-";
		StringBuilder sb = new StringBuilder();
		int cnt = 0;
		for (byte b : buf) {
			if (cnt++ > 100)
				break;
			int n = b & 0xff;
			if (n < 32 || n > 127)
				sb.append(".");
			else
				sb.append((char) n);
		}
		sb.append(" [");

		cnt = 0;
		for (byte b : buf) {
			if (cnt++ > 100)
				break;
			int n = b & 0xff;
			if (n <= 0xf)
				sb.append("0");
			sb.append(Integer.toHexString(n).toUpperCase());
			sb.append(" ");
		}
		sb.append("]");
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

	private static final Template TPL_QPIRI = new Template("<BBB.B> <CC.C> <DDD.D> <EE.E> <FF.F> <H+> <I+> <JJ.J> <KK.K> <LL.L> <MM.M> <NN.N> <O> <P+> <QQQ> <R> <S> <T> <r> <ss> <t> <U> <VV.V> <W> <X> <YYY>");

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

	private static final Template	TPL_QPIGS	= new Template("<BBB.B> <CC.C> <DDD.D> <EE.E> <F+> <G+> <HHH> <III> <JJ.JJ> <KKK> <OOO> <TTTT> <ee.e> <UUU.U> <WW.WW> <PPPPP> <AAAAAAAA> <QQ> <VV> <MMMMM> <ZZZ>");
	private static final Template	TPL_QPIGS2	= new Template("<AA.A> <BBB.B> <CCCCC>");

	public DeviceStatus getStatus() {
		DeviceStatus s = new DeviceStatus();
		String qp1 = query("QPIGS");
		String qp2 = query("QPIGS2");
		s.fromTemplateResultPart1(TPL_QPIGS.apply(qp1));
		s.fromTemplateResultPart2(TPL_QPIGS2.apply(qp2));
		lastQPIGS = "QPIGS: " + qp1 + "\nQPIGS2: " + qp2;
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

	public void setOutputMode(OutputMode mode) throws IOException {
		String resp;
		if (mode == OutputMode.SOL_BAT_UTIL)
			resp = query("POP02");
		else if (mode == OutputMode.UTIL_SOL_BAT)
			resp = query("POP00");
		else
			throw new IllegalArgumentException("Unknown output mode: " + mode);
		if (!"ACK".equals(resp))
			throw new IOException("Setting OutputMode (`POP` Command) failed with reponse: " + resp);
	}

	public String getLastQPIGS() {
		return lastQPIGS;
	}

	public void setFloatVoltage(float v) throws IOException {
		if (v < 48 || v > 63)
			throw new IOException("Invalid value: " + v);
		String resp = query("PBFT" + String.format("%04.1f", v));
		if (!"ACK".equals(resp))
			throw new IOException("Setting Float Voltage (`PBFT` Command) failed with reponse: " + resp);
	}
	
	public void setBulkVoltage(float v) throws IOException {
		if (v < 48 || v > 63)
			throw new IOException("Invalid value: " + v);
		String resp = query("PCVV" + String.format("%04.1f", v));
		if (!"ACK".equals(resp))
			throw new IOException("Setting Float Voltage (`PCVV` Command) failed with reponse: " + resp);
	}
	
}
