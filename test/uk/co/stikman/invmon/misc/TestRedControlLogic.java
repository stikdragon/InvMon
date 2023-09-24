package uk.co.stikman.invmon.misc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.controllers.RedControllerLogic;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.util.TestUtils;

public class TestRedControlLogic {
	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	@Test
	public void testStuff() throws InvMonException, IOException {
		Element el = loadTestConfigSection("Test1");
		RedControllerLogic x = new RedControllerLogic(null);
		File f = TestUtils.resourceToTempFile(this.getClass(), "red_csv_sample1.csv");
		el.setAttribute("csv", f.getAbsolutePath());
		x.config(el);

		x.run(LocalDateTime.parse("2023-01-04 15:00", dtf));

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
