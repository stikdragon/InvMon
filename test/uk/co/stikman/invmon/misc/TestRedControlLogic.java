package uk.co.stikman.invmon.misc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.controllers.RedControllerLogic;
import uk.co.stikman.invmon.controllers.RedControllerLogic.State;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.util.TestUtils;

//@formatter:off
public class TestRedControlLogic {
	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	@Test
	public void testStuff() throws InvMonException, IOException {
		File csv = TestUtils.resourceToTempFile(this.getClass(), "red_csv_sample1.csv");
		
		Element el = loadTestConfigSection("Test1");
		RedControllerLogic x = new RedControllerLogic(null);
		el.setAttribute("csv", csv.getAbsolutePath());
		x.config(el);

		//
		// run some scenarios to test the behaviour
		//
		// test data has threshold set to 50% on all days.  window is 1500-1800
		//
		Assert.assertEquals(State.NOT_CHARGING, x.run(LocalDateTime.parse("2023-01-04 14:00", dtf), 60)); // not in window; above thresh%
		Assert.assertEquals(State.NOT_CHARGING, x.run(LocalDateTime.parse("2023-01-04 14:10", dtf), 40)); // not in window
		Assert.assertEquals(State.CHARGING,     x.run(LocalDateTime.parse("2023-01-04 15:00", dtf), 40)); // in window, under threshold
		Assert.assertEquals(State.CHARGING,     x.run(LocalDateTime.parse("2023-01-04 15:10", dtf), 45)); // still going
		Assert.assertEquals(State.CHARGING,     x.run(LocalDateTime.parse("2023-01-04 15:20", dtf), 49)); // about to finish
		Assert.assertEquals(State.NOT_CHARGING, x.run(LocalDateTime.parse("2023-01-04 15:30", dtf), 55)); // above threshold
		Assert.assertEquals(State.NOT_CHARGING, x.run(LocalDateTime.parse("2023-01-04 15:40", dtf), 60)); 
		Assert.assertEquals(State.NOT_CHARGING, x.run(LocalDateTime.parse("2023-01-04 15:50", dtf), 40)); // already done it this window
		Assert.assertEquals(State.NOT_CHARGING, x.run(LocalDateTime.parse("2023-01-04 22:00", dtf), 40)); // not in window 
		Assert.assertEquals(State.CHARGING,     x.run(LocalDateTime.parse("2023-01-05 15:20", dtf), 40)); // next day, is allowed again
		
		
		//
		// do a similar thing but against a window that spans midnight, since that can be
		// non-trivial.  window is 2200-0200
		//
		el = loadTestConfigSection("Test2");
		x = new RedControllerLogic(null);
		el.setAttribute("csv", csv.getAbsolutePath());
		x.config(el);
		Assert.assertEquals(State.NOT_CHARGING, x.run(LocalDateTime.parse("2023-01-04 20:00", dtf), 60)); // not in window; above thresh%
		Assert.assertEquals(State.NOT_CHARGING, x.run(LocalDateTime.parse("2023-01-04 21:00", dtf), 40)); // not in window; below thresh%
		Assert.assertEquals(State.NOT_CHARGING, x.run(LocalDateTime.parse("2023-01-04 22:00", dtf), 60)); // not in window; above thresh%
		Assert.assertEquals(State.CHARGING,     x.run(LocalDateTime.parse("2023-01-04 22:00", dtf), 40)); // below thresh%
		Assert.assertEquals(State.CHARGING,     x.run(LocalDateTime.parse("2023-01-04 23:00", dtf), 40)); 
		Assert.assertEquals(State.CHARGING,     x.run(LocalDateTime.parse("2023-01-05 00:00", dtf), 40)); 
		Assert.assertEquals(State.CHARGING,     x.run(LocalDateTime.parse("2023-01-05 01:00", dtf), 45)); 
		Assert.assertEquals(State.NOT_CHARGING, x.run(LocalDateTime.parse("2023-01-05 01:01", dtf), 55)); 
		Assert.assertEquals(State.NOT_CHARGING, x.run(LocalDateTime.parse("2023-01-05 01:05", dtf), 45)); // still shouldn't charge as already done
		Assert.assertEquals(State.NOT_CHARGING, x.run(LocalDateTime.parse("2023-01-05 23:00", dtf), 55)); 
		Assert.assertEquals(State.CHARGING,     x.run(LocalDateTime.parse("2023-01-05 23:05", dtf), 45)); 
	}

	private Element loadTestConfigSection(String id) throws IOException {
		try (InputStream is = TestRedControlLogic.class.getResourceAsStream("controllertests1.xml")) {
			Document root = InvUtil.loadXML(is);
			for (Element el : InvUtil.getElements(root.getDocumentElement())) {
				if (el.getTagName().equals(id))
					return el;
			}
			throw new NoSuchElementException(id);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
