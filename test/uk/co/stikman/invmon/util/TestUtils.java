package uk.co.stikman.invmon.util;

import org.junit.Assert;
import org.junit.Test;

import uk.co.stikman.invmon.inverter.util.InvUtil;

public class TestUtils {
	@Test
	public void testSizeThing() {
		Assert.assertEquals("999 Kb", InvUtil.formatSize(999 * 1024));
		Assert.assertEquals("1500 Kb", InvUtil.formatSize(1500 * 1024));
		Assert.assertEquals("4096 Kb", InvUtil.formatSize(4 * 1024 * 1024));
		Assert.assertEquals("9 Mb", InvUtil.formatSize(9 * 1024 * 1024));
	}
}
