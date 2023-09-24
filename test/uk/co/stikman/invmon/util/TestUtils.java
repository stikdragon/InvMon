package uk.co.stikman.invmon.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.teavm.apachecommons.io.FileUtils;

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
			File f = File.createTempFile("invmontests", null);
			FileUtils.copyInputStreamToFile(is, f);
			return f;
		}
	}
}
