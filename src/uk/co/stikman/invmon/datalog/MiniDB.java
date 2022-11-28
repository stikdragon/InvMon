package uk.co.stikman.invmon.datalog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.datamodel.DataModel;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.log.StikLog;
import uk.co.stikman.table.DataRecord;
import uk.co.stikman.table.DataTable;

/**
 * Stores data in "blocks" of a given size. There's an "index" file which is the
 * main database file, this contains information about which record is in which
 * block. requesting a record from a block means it is loaded into memory. old
 * blocks get pushed out of this cache. An exception to this is that the most
 * recent block is always open
 * 
 * @author stik
 *
 */
public class MiniDB {
	private static final StikLog	LOGGER				= StikLog.getLogger(MiniDB.class);
	public static final int			DEFAULT_BLOCKSIZE	= 100000;
	private DataModel				model;
	private File					indexFile;
	private IndexFile				index;
	private List<Block>				blocks				= new ArrayList<>();
	private int						recordCount			= 0;
	private int						maxCachedBlocks		= 1;
	private final int				blockSize;
	private Set<Block>				open				= new HashSet<>();

	/**
	 * <code>file</code> is the main index file, block files will be called
	 * <code>file.1</code>, <code>file.2</code>, etc
	 * 
	 * @param file
	 */
	public MiniDB(File file, int blockSize) {
		this.indexFile = file;
		this.blockSize = blockSize;
	}

	public MiniDB(File file) {
		this(file, DEFAULT_BLOCKSIZE);
	}

	public void open() throws MiniDbException, ModelChangeException, InvMonException {

		//
		// read what we've got first
		//
		try {
			index = new IndexFile(this, indexFile);
			try {
				index.open();

				//
				// special case if we don't have a model assigned then we're meant to be
				// just opening our internal one without any checks
				//
				if (model != null) {
					if (index.getInternalModel() != null) {
						if (!model.equals(index.getInternalModel()))
							throw new ModelChangeException("Model has changed, database requires conversion");
					}
				} else { // model is null
					model = index.getInternalModel();
				}

			} catch (IOException e) {
				throw new MiniDbException(e);
			}

			//
			// create blocks
			//
			for (BlockInfo bi : index.getBlockInfo()) {
				Block b = new Block(this, bi, new File(indexFile.getAbsoluteFile() + "." + bi.getId()));
				blocks.add(b);
			}

			//
			// open last block
			//
			if (blocks.size() > 0) {
				Block b = blocks.get(blocks.size() - 1);
				b.open();
				recordCount = b.getInfo().getStartIndex() + b.getRecords().size();
			}
		} catch (Exception e) {
			index = null;
			throw e;
		}
	}

	/**
	 * create an entirely new block of records
	 * 
	 * @return
	 * @throws MiniDbException
	 */
	private Block newBlock(long ts) throws MiniDbException {
		Block prev = blocks.isEmpty() ? null : blocks.get(blocks.size() - 1);
		BlockInfo bi = new BlockInfo();
		bi.setId(blocks.size());
		bi.setStartIndex(recordCount * bi.getId());
		bi.setEndIndex(recordCount * (bi.getId() + 1) - 1);
		bi.setStartTS(prev == null ? 0 : prev.getInfo().getEndTS());
		bi.setEndTS(-1);
		Block b = new Block(this, bi, new File(indexFile.getAbsoluteFile() + "." + bi.getId()));
		b.open();
		blocks.add(b);
		index.addBlock(bi);
		return b;
	}

	public void close() throws IOException {
		synchronized (this) {
			for (Block b : blocks)
				b.close();
			if (index != null)
				index.close();
			index = null;
		}
	}

	public void setModel(DataModel model) {
		synchronized (this) {
			this.model = model;
		}
	}

	public void loadModel(InputStream is) throws IOException, InvMonException {
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

	public void commitRecord(DBRecord rec) throws MiniDbException {
		//
		// calculate fields
		//
		for (Field f : model.getCalculatedFields()) 
			rec.setFloat(f, f.getCalculationMethod().calc(rec));

		//
		// see if it'll fit in the current block
		//
		synchronized (this) {
			try {
				Block top = null;
				boolean create = blocks.isEmpty();
				if (!create) {
					top = blocks.get(blocks.size() - 1);
					create = top.isFull();
				}
				if (create) {
					top = newBlock(rec.getTimestamp());
					index.save();
				}
				rec.setIndex(recordCount++);
				top.add(rec);
			} catch (IOException e) {
				throw new MiniDbException("Commit failed: " + e.getMessage(), e);
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
			dt.addFields("ID", "Min", "Max", "MinTS", "MaxTS");

			for (Block b : blocks) {
				DataRecord r = dt.addRecord();
				r.setValue(0, b.getInfo().getId());
				r.setValue(1, b.getInfo().getStartIndex());
				r.setValue(2, b.getInfo().getEndIndex());
				r.setValue(3, b.getInfo().getStartTS());
				r.setValue(4, b.getInfo().getEndTS());
			}

			//			for (Field f : model)
			//				dt.addField(f.getId());

			//			for (DBRecord r : records) {
			//				DataRecord r2 = dt.addRecord();
			//				int i = 0;
			//				for (Field f : model) {
			//					r2.setValue(i, f.toString(r));
			//					++i;
			//				}
			//			}
			return dt;
		}
	}

	/**
	 * given an int or a long field it finds a range of records. we look at the
	 * blocks that contain it and try to load them all. it's possible for this to
	 * fail if you ask for a range that would span more than
	 * <code>maxCachedBlocks</code>
	 * 
	 * @param field
	 * @param start
	 * @param end
	 * @return
	 * @throws MiniDbException
	 */
	public IntRange getRecordRange(long start, long end) throws MiniDbException {
		IntRange res = new IntRange(-1, -1);
		synchronized (this) {
			Block first = null;
			Block last = null;
			for (Block block : blocks) {
				if (block.containsTS(start))
					first = block;
				if (block.containsTS(end))
					last = block;
				if (last != null && first != null)
					break;
			}

			if (first == null) {
				if (blocks.get(0).getInfo().getStartTS() > start)
					first = blocks.get(0);
			}
			if (last == null) {
				if (blocks.get(blocks.size() - 1).getInfo().getEndIndex() < end)
					last = blocks.get(blocks.size() - 1);
			}

			if (first == null || last == null)
				throw new MiniDbException("Could not identify block range for [" + start + ", " + end + "]");

			//
			// now we need to find the index of the specific records in each of those blocks
			//
			openBlock(first);
			for (DBRecord r : first.getRecords()) {
				long v = r.getTimestamp();
				if (v >= start) {
					res.setLow(r.getIndex());
					break;
				}
			}

			openBlock(last);
			for (DBRecord r : last.getRecords()) {
				long v = r.getTimestamp();
				if (v > end) {
					res.setHigh(r.getIndex() - 1);
					break;
				}
			}

			if (res.getHigh() == -1)
				res.setHigh(recordCount - 1);

		}
		return res;
	}

	private Block openBlock(int id) throws MiniDbException {
		Block b = blocks.get(id);
		openBlock(b);
		return b;
	}

	/**
	 * Makes sure this block is loaded. Loading a block will push out the oldest one
	 * from the cache if it's over
	 * 
	 * @return
	 * 
	 * @throws MiniDbException
	 */
	private void openBlock(Block b) throws MiniDbException {
		if (!b.isOpen()) {
			b.open();
			b.setLastAccessed(System.currentTimeMillis());
			open.add(b);
		}
		testCacheSize();
		if (!b.isOpen())
			throw new MiniDbException("Could not open block [" + b + "] because the cache is full");
	}

	private void testCacheSize() throws MiniDbException {
		while (open.size() > maxCachedBlocks)
			closeOldestBlock();
	}

	private void closeOldestBlock() throws MiniDbException {
		long oldestT = Long.MAX_VALUE;
		Block oldestB = null;
		for (Block b : open) {
			if (b.getLastAccessed() < oldestT) {
				oldestT = b.getLastAccessed();
				oldestB = b;
			}
		}
		if (oldestB != null)
			closeBlock(oldestB);
	}

	private void closeBlock(Block block) throws MiniDbException {
		if (!block.isOpen())
			return;
		try {
			block.close();
		} catch (IOException e) {
			throw new MiniDbException(e);
		}
		open.remove(block);
	}

	public int getMaxCachedBlocks() {
		return maxCachedBlocks;
	}

	public void setMaxCachedBlocks(int maxCachedBlocks) {
		this.maxCachedBlocks = maxCachedBlocks;
	}

	public int getBlockSize() {
		return blockSize;
	}

	public DBRecord getRecord(int idx) throws MiniDbException {
		int blockid = idx / blockSize;
		Block b = openBlock(blockid);
		return b.getRecords().get(idx - blockid * blockSize);
	}

	public String recordToString(DBRecord rec) {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (Field f : model) {
			sb.append(sep);
			switch (f.getDataType()) {
				case FLOAT:
					sb.append(rec.getFloat(f));
					break;
			}
			sep = ", ";
		}
		return sb.toString();
	}

	/**
	 * returns <code>null</code> if there's no records yet
	 * 
	 * @return
	 * @throws MiniDbException
	 */
	public DBRecord getLastRecord() throws MiniDbException {
		if (recordCount == 0)
			return null;
		return getRecord(recordCount - 1);
	}

	public int getRecordCount() {
		return recordCount;
	}

	public boolean isOpen() {
		return index != null;
	}

	/**
	 * rename the index file, and all the blocks
	 * 
	 * @param file
	 * @throws ModelChangeException
	 * @throws MiniDbException
	 * @throws IOException
	 * @throws InvMonException
	 */
	public void rename(File file) throws MiniDbException, ModelChangeException, IOException, InvMonException {
		if (isOpen())
			throw new IllegalStateException("MiniDB must be closed for a rename operation");

		//
		// get list of all blocks
		//
		Map<File, File> pairs = new HashMap<>();
		open();
		try {
			pairs.put(this.indexFile, file);
			for (Block b : blocks)
				pairs.put(b.getFile(), new File(file.getAbsoluteFile() + "." + b.getInfo().getId()));
		} finally {
			close();
		}

		for (Entry<File, File> pair : pairs.entrySet())
			pair.getKey().renameTo(pair.getValue());
	}

	public int getOpenBlocks() {
		return open.size();
	}

}
