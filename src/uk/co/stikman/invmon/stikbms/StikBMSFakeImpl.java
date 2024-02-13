package uk.co.stikman.invmon.stikbms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;

public class StikBMSFakeImpl implements StikBMSInterface {

	private boolean closed;

	public StikBMSFakeImpl(SerialPort port, int baud) {
	}

	@Override
	public int queryProtocol() throws IOException {
		return 1;
	}

	@Override
	public String queryVersion() throws IOException {
		return "fake";
	}

	@Override
	public void close() throws IOException {
		closed = true;
	}

	@Override
	public List<CalibFactor> getCalibFactors() throws IOException {
		List<CalibFactor> lst = new ArrayList<>();
		for (int i = 0; i < 16; ++i)
			lst.add(new CalibFactor("c" + i, 1.0f));
		return lst;
	}

	@Override
	public void resetCalib() throws IOException {

	}

	@Override
	public void setCalibFactor(CalibTarget tgt, int i, float f) throws IOException {

	}

	@Override
	public BMSMetrics queryMetrics() throws IOException {
		BMSMetrics m = new BMSMetrics();
		m.setTemperatures(new float[] { 23, 26, 43 });
		float[] vs = new float[16];
		for (int i = 0; i < 16; ++i)
			vs[i] = i * 3.2f;
		m.setVoltages(vs);
		m.setCurrent(45.2f);
		return m;
	}

	@Override
	public void open() throws IOException {
		closed = false;
	}

}
