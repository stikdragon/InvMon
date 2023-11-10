package uk.co.stikman.invmon.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.teavm.apachecommons.io.FileUtils;

import uk.co.stikman.invmon.RateLimiter;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.utils.StreamUtil;

public class TestUtils {
	@Test
	public void testSizeThing() {
		Assert.assertEquals("999 Kb", InvUtil.formatSize(999 * 1024));
		Assert.assertEquals("1500 Kb", InvUtil.formatSize(1500 * 1024));
		Assert.assertEquals("4096 Kb", InvUtil.formatSize(4 * 1024 * 1024));
		Assert.assertEquals("9 Mb", InvUtil.formatSize(9 * 1024 * 1024));
	}

	public static File resourceToTempFile(Class<?> cls, String name) throws IOException {
		try (InputStream is = cls.getResourceAsStream(name)) {
			if (is == null)
				throw new FileNotFoundException(name);
			File f = File.createTempFile("invmontests", null);
			FileUtils.copyInputStreamToFile(is, f);
			return f;
		}
	}

	//@formatter:off
	@Test
	public void testRateLimiter() {
		long[] time = new long[1];
		RateLimiter x = new RateLimiter(5, 10, () -> time[0]); // 5 events during 10s
		
		time[0] = 1000;		x.trigger();    assertFalse(x.test());
		time[0] = 1100;		x.trigger();    assertFalse(x.test());
		time[0] = 1200;		x.trigger();    assertFalse(x.test());
		time[0] = 1300;		x.trigger();    assertFalse(x.test());
		time[0] = 1400;		x.trigger();    assertTrue(x.test());
		time[0] = 1500;		x.trigger();    assertTrue(x.test());
		
		x = new RateLimiter(5, 1, () -> time[0]); // 5 events during 1s
		
		time[0] = 1000;		x.trigger();    assertFalse(x.test());
		time[0] = 1100;		x.trigger();    assertFalse(x.test());
		time[0] = 1200;		x.trigger();    assertFalse(x.test());
		time[0] = 1300;		x.trigger();    assertFalse(x.test());
		time[0] = 2001;		x.trigger();    assertFalse(x.test());
		time[0] = 2500;		x.trigger();    assertFalse(x.test());
		time[0] = 2600;		x.trigger();    assertFalse(x.test());
		time[0] = 2700;		x.trigger();    assertFalse(x.test());
		time[0] = 2800;		x.trigger();    assertTrue(x.test());

	}
	//@formatter:on

}
