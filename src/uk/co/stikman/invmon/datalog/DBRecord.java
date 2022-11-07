package uk.co.stikman.invmon.datalog;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import uk.co.stikman.invmon.datamodel.Field;

public class DBRecord {
	private int			index;
	private ByteBuffer	buffer;

	public DBRecord(MiniDB owner) {
		super();
		buffer = ByteBuffer.allocate(owner.getModel().getRecordWidth());
	}

	public int getIndex() {
		return index;
	}

	void setIndex(int index) {
		this.index = index;
	}

	public ByteBuffer getBuffer() {
		return buffer;
	}

	public void set(Field field, long f) {
		buffer.putLong(field.getOffset(), f);
	}

	public void set(Field field, float f) {
		buffer.putInt(field.getOffset(), Float.floatToRawIntBits(f));
	}

	public void set(Field field, int n) {
		buffer.putInt(field.getOffset(), n);
	}

	public void set(Field field, String s) {
		byte[] b = s.getBytes(StandardCharsets.ISO_8859_1);
		int n = b.length;
		if (n > field.getWidth())
			n = field.getWidth();
		buffer.put(field.getOffset(), b, 0, n);
		if (n < field.getWidth())
			buffer.put(field.getOffset() + n, (byte) 0);
	}

	public void copyData(byte[] buf) {
		buffer.rewind();
		buffer.put(buf);
		buffer.rewind();
	}

	public float getFloat(Field field) {
		//
		// calculate fields if necessary
		//
		if (field.getCalculationMethod() != null)
			return field.getCalculationMethod().calc(this);
		else
			return Float.intBitsToFloat(buffer.getInt(field.getOffset()));
	}

	public String getString(Field field) {
		byte[] arr = new byte[field.getWidth()];
		buffer.get(field.getOffset(), arr);
		int p = 0;
		for (int i = 0; i < field.getWidth(); ++i) {
			if (arr[i] == 0) {
				p = i;
				break;
			}
		}
		return new String(arr, 0, p, StandardCharsets.ISO_8859_1);
	}

	public int getInt(Field field) {
		return buffer.getInt(field.getOffset());
	}

	public long getLong(Field field) {
		return buffer.getLong(field.getOffset());
	}

}
