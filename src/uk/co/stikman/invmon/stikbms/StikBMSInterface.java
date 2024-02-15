package uk.co.stikman.invmon.stikbms;

import java.io.IOException;
import java.util.List;

public interface StikBMSInterface {

	int queryProtocol() throws IOException;
	String queryVersion() throws IOException;

	void close() throws IOException;

	List<CalibFactor> getCalibFactors() throws IOException;

	void resetCalib() throws IOException;

	void setCalibFactor(CalibTarget tgt, int i, float f) throws IOException;

	BMSMetrics queryMetrics() throws IOException;

	void open() throws IOException;


}
