package uk.co.stikman.invmon.datalog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import uk.co.stikman.log.StikLog;

public class Block {
	private static final StikLog	LOGGER			= StikLog.getLogger(Block.class);
	private static final int		MAGIC_NUMBER	= 0x4D9B5FA9;
	private static final int		VERSION_1		= 1;
	private List<DBRecord>			records			= new ArrayList<>();
	private DataOutputStream		output			= null;
	private File					file;
	private MiniDB					owner;
	private BlockInfo				info;
	private long					lastAccessed	= 0;

	public long getLastAccessed() {
		return lastAccessed;
	}

	public void setLastAccessed(long lastAccessed) {
		this.lastAccessed = lastAccessed;
	}

	public Block(MiniDB owner, BlockInfo info, File file) {
		this.owner = owner;
		this.info = info;
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	public boolean isOpen() {
		return output != null;
	}

	public void open() throws MiniDbException {
		LOGGER.debug("Opening block " + this);
		//
		// read contents first
		//
		if (file.exists()) {
			try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
				if (dis.readInt() != MAGIC_NUMBER)
					throw new IOException("Stream is not a database file");
				int ver = dis.readInt();
				if (ver != VERSION_1)
					throw new IOException("Unsupported file version: " + ver);

				int idx = info.getStartIndex();
				int n;
				for (;;) {
					try {
						n = dis.readByte();
					} catch (EOFException eofe) {
						break;
					}
					if (n == -1)
						break;
					if (n == 1) { // marks a record
						DBRecord r = new DBRecord(owner);
						r.setIndex(idx++);
						r.fromStream(dis);
						records.add(r);
					} else
						throw new IOException("Corrupt file");
				}
			} catch (IOException e) {
				throw new MiniDbException("Failed to open block: " + e.getMessage(), e);
			}
		} else {
			//
			// write initial file structure
			//
			try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
				dos.writeInt(MAGIC_NUMBER);
				dos.writeInt(VERSION_1);
			} catch (IOException e) {
				throw new MiniDbException("Failed to open block: " + e.getMessage(), e);
			}
		}

		//
		// now write to it
		//
		try {
			output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file, true)));
		} catch (IOException e) {
			throw new MiniDbException("Failed to open block: " + e.getMessage(), e);
		}
	}

	public void close() throws IOException {
		LOGGER.debug("Closing block " + this);
		output.close();
		output = null;
	}

	public boolean isFull() {
		return records.size() >= owner.getBlockSize();
	}

	public void add(DBRecord rec) throws MiniDbException {
		info.setEndIndex(rec.getIndex());
		info.setEndTS(rec.getTimestamp());
		records.add(rec);

		try {
			output.write(1); // "record"
			rec.toStream(output);
			output.flush();
		} catch (IOException e) {
			throw new MiniDbException("Failed to write record: " + e.getMessage(), e);
		}
	}

	public BlockInfo getInfo() {
		return info;
	}

	public boolean containsTS(long ts) {
		if (info.getEndTS() == -1) // special case when we're the top block
			return ts >= info.getStartTS();
		return (ts >= info.getStartTS()) && (ts < info.getEndTS());
	}

	public static int compareAge(Block a, Block b) {
		return Long.compare(a.lastAccessed, b.lastAccessed);
	}

	public List<DBRecord> getRecords() {
		return records;
	}

	@Override
	public String toString() {
		return Integer.toString(getInfo().getId());
	}

}
