package uk.co.stikman.invmon;

public interface RecordListener {
	void record(long id, InverterDataPoint rec);
}
