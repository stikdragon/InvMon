package uk.co.stikman.invmon.datalog;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.datamodel.FieldCounts;
import uk.co.stikman.invmon.datamodel.FieldType;
import uk.co.stikman.invmon.datamodel.FieldVIF;
import uk.co.stikman.invmon.datamodel.VIFReading;

public class DBRecord {
	private int			index;
	private long		timestamp;
	private float[]		floats;
	private int[]		ints;
	private String[]	strings;
	private byte[]		bytes;

	public DBRecord(MiniDB owner) {
		super();
		FieldCounts fc = owner.getModel().getFieldCounts();

		floats = fc.floats == 0 ? null : new float[fc.floats];
		ints = fc.ints == 0 ? null : new int[fc.ints];
		strings = fc.strings == 0 ? null : new String[fc.strings];
		bytes = fc.bytes == 0 ? null : new byte[fc.bytes];
	}

	public int getIndex() {
		return index;
	}

	void setIndex(int index) {
		this.index = index;
	}

	public void setFloat(Field field, float f) {
		floats[field.getPosition()] = f;
	}

	public void setInt(Field field, int n) {
		ints[field.getPosition()] = n;
	}

	public void setString(Field field, String s) {
		strings[field.getPosition()] = s.intern();
	}

	public float getFloat(Field field) {
		return floats[field.getPosition()];
	}

	public String getString(Field field) {
		return strings[field.getPosition()];
	}

	public int getInt(Field field) {
		return ints[field.getPosition()];
	}

	public float getFloat8(Field field) {
		float n = Byte.toUnsignedInt(bytes[field.getPosition()]);
		FieldType t = field.getType();
		float a = t.max() - t.min();
		return t.min() + a * n / 256.0f;
	}

	public void setFloat8(Field field, float f) {
		FieldType t = field.getType();
		f -= t.min();
		f = 256.0f * f / (t.max() - t.min());
		bytes[field.getPosition()] = (byte) f;
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

	public void fromStream(DataInputStream dis, int dbver) throws IOException {
		if (dbver == 1) {
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
						strings[i3++] = new String(b, StandardCharsets.ISO_8859_1).intern();
						break;
				}
			}
		} else if (dbver == 2) {
			//
			// in ver 2 they're all stored as a list of ints
			//

		} else
			throw new RuntimeException("Unexpected version: " + dbver);
	}

	public VIFReading getVIF(FieldVIF vif) {
		float v = vif.getV() != null ? getFloat(vif.getV()) : 0.0f;
		float i = vif.getI() != null ? getFloat(vif.getI()) : 0.0f;
		float f = vif.getF() != null ? getFloat(vif.getF()) : 0.0f;
		return new VIFReading(v, i, f);
	}

	public <T extends Enum<T>> T getEnum(Field f, Class<T> cls) {
		return (T) Enum.valueOf(cls, getString(f));
	}

}
