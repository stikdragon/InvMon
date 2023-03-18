package uk.co.stikman.invmon.remote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.w3c.dom.Element;

import uk.co.stikman.eventbus.Subscribe;
import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.Events;
import uk.co.stikman.invmon.InvModule;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datalog.DataLogger;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.log.StikLog;

public class JSONSend extends InvModule {
	private static final StikLog	LOGGER				= StikLog.getLogger(JSONSend.class);
	private static final int		MAX_MESSAGE_SIZE	= 1024 * 16;
	private String					destinationAddress;
	private int						destinationPort;
	private String					key;
	private DataLogger				targetLogger;
	private Thread					thread;
	private boolean					terminated;
	private String					user;

	public JSONSend(String id, Env env) {
		super(id, env);
	}

	@Override
	public void configure(Element config) {
		this.destinationAddress = InvUtil.getAttrib(config, "destination");
		this.destinationPort = Integer.parseInt(InvUtil.getAttrib(config, "port"));
		this.key = InvUtil.getAttrib(config, "key");
		this.user = InvUtil.getAttrib(config, "user");
		this.targetLogger = getEnv().getModule(InvUtil.getAttrib(config, "logger"));
	}

	public String getDestinationAddress() {
		return destinationAddress;
	}

	public int getDestinationPort() {
		return destinationPort;
	}

	public String getKey() {
		return key;
	}

	@Subscribe(Events.LOGGER_RECORD_COMMITED)
	public void postData(DBRecord data) {
	}

	private void loop() {
		EncryptHelper enc = new EncryptHelper(getKey());

		for (;;) {
			LOGGER.info("Connecting to [" + destinationAddress + ":" + destinationPort);
			try (Socket socket = new Socket(destinationAddress, destinationPort)) {
				LOGGER.info("  ..connected");
				DataOutputStream sockOs = new DataOutputStream(socket.getOutputStream());
				DataInputStream sockIs = new DataInputStream(socket.getInputStream());

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);

				//
				// init
				//
				dos.write("SK".getBytes(StandardCharsets.ISO_8859_1));
				dos.write(user.getBytes(StandardCharsets.ISO_8859_1));
				sockOs.write(baos.toByteArray());
				DataInputStream resp = readResp(sockIs);

				for (;;) {
					if (terminated)
						return;

					//
					// ask the destination what the last record they've seen is
					//

					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			} catch (IOException e1) {
				LOGGER.error("JSON connection failed: " + e1.getMessage(), e1);
			}

		}
	}

	private byte[] recvbuffer = new byte[128];

	/**
	 * throws {@link IOException} if server raises error
	 * 
	 * @param src
	 * @return
	 * @throws IOException
	 */
	private DataInputStream readResp(DataInputStream src) throws IOException {
		int len = src.read();
		if (len > MAX_MESSAGE_SIZE)
			throw new IOException("Invalid response from server (too long)");
		if (len > recvbuffer.length)
			recvbuffer = new byte[len];
		src.readNBytes(recvbuffer, 0, len);
		DataInputStream res = new DataInputStream(new ByteArrayInputStream(recvbuffer, 0, len));
		int ch = res.read();
		if (ch == 'E')
			throw new IOException("Error from server: " + new String(res.readNBytes(len - 1), StandardCharsets.ISO_8859_1));
		if (ch == 'A')
			return res;

		throw new IOException("Unknown response from server: 0x" + Integer.toString(ch, 16));
	}

	@Override
	public void start() throws InvMonException {
		super.start();
		thread = new Thread(this::loop);
		try {
			thread.join();
		} catch (InterruptedException e) {
			LOGGER.warn("join() was interrupted");
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void terminate() {
		terminated = true;
		if (thread != null)
			thread.interrupt();
		super.terminate();
	}

}
