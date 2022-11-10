package uk.co.stikman.invmon.datalog;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.datamodel.FieldCounts;
import uk.co.stikman.invmon.datamodel.FieldDataType;

public class DBRecord {
	private int			index;
	private long		timestamp;
	private float[]		floats;
	private int[]		ints;
	private String[]	strings;

	public DBRecord(MiniDB owner) {
		super();
		FieldCounts fc = owner.getModel().getFieldCounts();
		floats = fc.floats == 0 ? null : new float[fc.floats];
		ints = fc.ints == 0 ? null : new int[fc.ints];
		strings = fc.strings == 0 ? null : new String[fc.strings];
	}

	public int getIndex() {
		return index;
	}

	void setIndex(int index) {
		this.index = index;
	}

	public void setFloat(Field field, float f) {
		checkFieldType(field, FieldDataType.FLOAT);
		floats[field.getPosition()] = f;
	}

	public void setInt(Field field, int n) {
		checkFieldType(field, FieldDataType.INT);
		ints[field.getPosition()] = n;
	}

	public void setString(Field field, String s) {
		checkFieldType(field, FieldDataType.STRING);
		strings[field.getPosition()] = s;
	}

	private static void checkFieldType(Field f, FieldDataType typ) {
		if (f.getDataType() != typ)
			throw new IllegalArgumentException("Field [" + f.getId() + "] is not of type " + typ);
	}

	public float getFloat(Field field) {
		checkFieldType(field, FieldDataType.FLOAT);
		//
		// calculate fields if necessary
		//
		if (field.getCalculationMethod() != null)
			return field.getCalculationMethod().calc(this);
		else
			return floats[field.getPosition()];
	}

	public String getString(Field field) {
		checkFieldType(field, FieldDataType.STRING);
		return strings[field.getPosition()];
	}

	public int getInt(Field field) {
		checkFieldType(field, FieldDataType.INT);
		return ints[field.getPosition()];
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void toStream(DataOutputStream output) throws IOException {
		output.writeLong(timestamp);
		if (ints != null) {
			for (int i : ints) {
				output.write(1);
				output.writeInt(i);
			}
		}
		if (floats != null) {
			for (float f : floats) {
				output.write(2);
				output.writeFloat(f);
			}
		}
		if (strings != null) {
			for (String s : strings) {
				output.write(3);
				byte[] b = s.getBytes(StandardCharsets.ISO_8859_1);
				if (b.length > 255)
					throw new IOException("String data too long");
				output.write(b.length);
				output.write(b);
			}
		}
		output.write(0);
	}

	public void fromStream(DataInputStream dis) throws IOException {
		timestamp = dis.readLong();
		int i1 = 0;
		int i2 = 0;
		int i3 = 0;
		for (;;) {
			int n = dis.read();
			switch (n) {
				case 0:
					return;
				case 1:
					ints[i1++] = dis.readInt();
					break;
				case 2:
					floats[i2++] = dis.readFloat();
					break;
				case 3:
					int a = dis.read();
					byte[] b = new byte[a];
					dis.readFully(b);
					strings[i3++] = new String(b, StandardCharsets.ISO_8859_1);
					break;
			}
		}
	}

}
