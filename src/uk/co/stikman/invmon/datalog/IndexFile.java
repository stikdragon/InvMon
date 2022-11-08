package uk.co.stikman.invmon.datalog;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import uk.co.stikman.invmon.datamodel.DataModel;

public class IndexFile {
	private static final int	VERSION_1		= 1;
	private static final int	MAGIC_NUMBER	= 0x15C4238E;
	private MiniDB				owner;
	private File				file;
	private List<BlockInfo>		blockInfo		= new ArrayList<>();

	public IndexFile(MiniDB owner, File file) {
		this.owner = owner;
		this.file = file;
	}

	public void open() throws IOException {
		boolean exists = file.exists();
		if (exists) {
			try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
				read(is);
			}
		}
	}

	private void read(InputStream is) throws IOException {
		try (DataInputStream dis = new DataInputStream(is)) {
			if (dis.readInt() != MAGIC_NUMBER)
				throw new IOException("Stream is not a database file");
			int ver = dis.readInt();
			if (ver != VERSION_1)
				throw new IOException("Unsupported file version: " + ver);

			int blocksize = dis.readInt();
			if (owner.getBlockSize() != blocksize)
				throw new IOException("Database Block Size does not match ([" + blocksize + "] vs [" + owner.getBlockSize() + "])");

			DataModel existing = new DataModel();
			int n = dis.readInt();
			byte[] buf = new byte[n];
			dis.readFully(buf);
			try (ByteArrayInputStream bais = new ByteArrayInputStream(buf)) {
				existing.loadXML(bais);
			}

			if (!existing.equals(owner.getModel())) // TODO: convert
				throw new IOException("Model version different");

			//
			// read block info
			//
			for (;;) {
				n = dis.read();
				if (n == 0x0)
					return;
				if (n == 0x1) { // block details
					BlockInfo bi = new BlockInfo();
					bi.setId(dis.readInt());
					bi.setStartTS(dis.readLong());
					bi.setEndTS(dis.readLong());

					bi.setStartIndex(bi.getId() * owner.getBlockSize());
					bi.setEndIndex((bi.getId() + 1) * owner.getBlockSize() - 1);
					blockInfo.add(bi);
				}
			}
		}
	}

	public List<BlockInfo> getBlockInfo() {
		return blockInfo;
	}

	public void close() throws IOException {
	}

	public void addBlock(BlockInfo bi) {
		blockInfo.add(bi);
		try {
			save();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void save() throws IOException {
		try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
			dos.writeInt(MAGIC_NUMBER);
			dos.writeInt(VERSION_1);
			dos.writeInt(owner.getBlockSize());

			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				owner.getModel().writeXML(baos);
				byte[] bytes = baos.toByteArray();
				dos.writeInt(bytes.length);
				dos.write(bytes);
			}

			for (BlockInfo bi : blockInfo) {
				dos.writeByte(0x1);
				dos.writeInt(bi.getId());
				dos.writeLong(bi.getStartTS());
				dos.writeLong(bi.getEndTS());
			}
			dos.writeByte(0x0);
		}
	}
}
