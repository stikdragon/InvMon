package uk.co.stikman.invmon.datalog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.table.DataRecord;
import uk.co.stikman.table.DataTable;

/**
 * Stores data in "blocks" of a given size. There's an "index" file which is the
 * main database file, this contains information about which record is in which
 * block. requesting a record from a block means it is loaded into memory. old
 * blocks get pushed out of this cache
 * 
 * @author stik
 *
 */
public class MiniDB {
	private static final int	VERSION_1		= 1;
	private static final int	MAGIC_NUMBER	= 0x15C4238E;
	private DataModel			model;
	private List<DBRecord>		records			= new ArrayList<>();
	private File				outputFile;
	private ByteBuffer			markerBuffer;
	private DataOutputStream	output;

	public MiniDB(File file) {
		this.outputFile = file;
		markerBuffer = ByteBuffer.allocate(1);
		markerBuffer.put((byte) 1);
	}

	public void open() throws IOException {
		//
		// read what we've got first
		//
		boolean exists = outputFile.exists();
		if (exists) {
			try (InputStream is = new BufferedInputStream(new FileInputStream(outputFile))) {
				read(is);
			}
		}
		if (!exists) {
			//
			// write initial file
			//
			try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(outputFile))) {
				dos.writeInt(MAGIC_NUMBER);
				dos.writeInt(VERSION_1);

				try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					model.writeXML(baos);
					byte[] bytes = baos.toByteArray();
					dos.writeInt(bytes.length);
					dos.write(bytes);
				}
			}
		}

		output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile, true)));
	}

	private void read(InputStream is) throws IOException {
		try (DataInputStream dis = new DataInputStream(is)) {
			if (dis.readInt() != MAGIC_NUMBER)
				throw new IOException("Stream is not a database file");
			int ver = dis.readInt();
			if (ver != VERSION_1)
				throw new IOException("Unsupported file version: " + ver);
			DataModel existing = new DataModel();
			int n = dis.readInt();
			byte[] buf = new byte[n];
			dis.readFully(buf);
			try (ByteArrayInputStream bais = new ByteArrayInputStream(buf)) {
				existing.loadXML(bais);
			}

			if (!existing.equals(model)) // TODO: convert
				throw new IOException("Model version different");

			int idx = 0;
			byte[] recbuf = new byte[model.getRecordWidth()];
			for (;;) {
				try {
					n = dis.readByte();
				} catch (EOFException eofe) {
					break;
				}
				if (n == -1)
					break;
				if (n == 1) { // marks a record
					dis.readFully(recbuf);
					DBRecord r = new DBRecord(this);
					r.setIndex(idx++);
					r.copyData(recbuf);
					records.add(r);
				} else
					throw new IOException("Corrupt file");
			}
		}

	}

	public void close() throws IOException {
		synchronized (this) {
			output.close();
			output = null;
		}
	}

	public void setModel(DataModel model) {
		synchronized (this) {
			this.model = model;
		}
	}

	public void loadModel(InputStream is) throws IOException {
		synchronized (this) {
			model = new DataModel();
			model.loadXML(is);
		}
	}

	public DataModel getModel() {
		return model;
	}

	public DBRecord addRecord() {
		return new DBRecord(this);
	}

	public void commitRecord(DBRecord rec) {

		synchronized (this) {
			//
			// trim any strings
			//
			rec.setIndex(records.size());
			records.add(rec);
			try {
				output.write(1); // "record"
				output.write(rec.getBuffer().array());
				output.flush();
				System.out.println("size: " + outputFile.length());
			} catch (IOException e) {
				throw new RuntimeException("Failed to write record: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * this is a debugging method, really
	 * 
	 * @return
	 */
	public DataTable toDataTable() {
		synchronized (this) {
			DataTable dt = new DataTable();
			for (Field f : model)
				dt.addField(f.getId());

			for (DBRecord r : records) {
				DataRecord r2 = dt.addRecord();
				int i = 0;
				for (Field f : model) {
					r2.setValue(i, f.toString(r));
					++i;
				}
			}
			return dt;
		}
	}

	/**
	 * given an int or a long field it finds a range of records
	 * 
	 * @param field
	 * @param start
	 * @param end
	 * @return
	 */
	public IntRange getRecordRange(Field field, long start, long end) {
		IntRange res = new IntRange(-1, -1);
		synchronized (this) {
			for (DBRecord r : records) {
				long v = r.getLong(field);
				if (res.getLow() == -1 && start <= v)
					res.setLow(r.getIndex());
				if (v > end)
					res.setHigh(r.getIndex());
				if (res.getHigh() != -1 && res.getLow() != -1)
					return res;
			}
			if (res.getHigh() == -1)
				res.setHigh(records.size() - 1);
		}
		return res;
	}

	public DBRecord getRecord(int idx) {
		return records.get(idx);
	}

}
