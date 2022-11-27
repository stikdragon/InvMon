package uk.co.stikman.invmon.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datalog.IntRange;
import uk.co.stikman.invmon.datalog.MiniDB;
import uk.co.stikman.invmon.datalog.MiniDbException;
import uk.co.stikman.invmon.datalog.ModelChangeException;
import uk.co.stikman.invmon.datamodel.DataModel;

public class TestMiniDB {

	@Test
	public void testOpenClose() throws IOException, MiniDbException, ModelChangeException, InvMonException {
		DataModel model = new DataModel();
		model.loadXML(getClass().getResourceAsStream("testmodel1.xml"));

		File tmpf = File.createTempFile("test1", ".db");
		tmpf.delete();
		MiniDB db = new MiniDB(tmpf);
		db.setModel(model);
		db.open();
		db.close();

		db = new MiniDB(tmpf);
		db.setModel(model);
		db.open();
		db.close();
	}

	@Test
	public void testTinyBlocks() throws IOException, MiniDbException, ModelChangeException, InvMonException {
		DataModel model = new DataModel();
		model.loadXML(getClass().getResourceAsStream("testmodel1.xml"));

		File tmpf = File.createTempFile("test1", ".db");
		tmpf.delete();
		System.out.println("testtinyBlocks, file=" + tmpf);
		MiniDB db = new MiniDB(tmpf, 4);

		db.setModel(model);
		db.open();

		for (int i = 0; i < 100; ++i) {
			DBRecord r = db.addRecord();
			r.setTimestamp(i * 100);
			r.setFloat(model.get("F1"), i);
			r.setFloat(model.get("F2"), i * 2);
			r.setFloat(model.get("F3"), i + 10);
			db.commitRecord(r);
		}
		db.close();

		db = new MiniDB(tmpf, 4);
		db.setModel(model);
		db.open();

		testRangeTotal(db, model, 0, 9900, "F3", 5950);
		testRangeTotal(db, model, 1004, 1504, "F3", 115);
		testRangeTotal(db, model, 9700, 9800, "F3", 215);
		testRangeTotal(db, model, 9701, 9799, "F3", 0); // should be no records
		testRangeTotal(db, model, 9700, 9799, "F3", 107); // should be just 1
		testRangeTotal(db, model, 9701, 9800, "F3", 108); // should be just 1
	}

	private void testRangeTotal(MiniDB db, DataModel model, int low, int hi, String field, int total) throws MiniDbException {
		long x = 0;
		IntRange rng = db.getRecordRange(low, hi);
		for (int i = rng.getLow(); i <= rng.getHigh(); ++i) {
			DBRecord rec = db.getRecord(i);
			x += rec.getFloat(model.get(field));
		}
		assertEquals(total, x);

	}

}
