package uk.co.stikman.invmon.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import uk.co.stikman.invmon.inverter.util.Format;

public class TestStringFormatter {
	@Test
	public void testFormat1() {
		Format f = new Format("hello");
		assertEquals("hello", f.format());

		f = new Format("test %f blah");
		assertEquals("test 123.0 blah", f.format(123));

		f = new Format("test %d blah");
		assertEquals("test 123 blah", f.format(123));

		f = new Format("test %5d blah");
		assertEquals("test   123 blah", f.format(123));

		f = new Format("test %3.5f blah");
		assertEquals("test   1.00000 blah", f.format(1));

		f = new Format("%d %d %d");
		assertEquals("-1 2 3", f.format(-1, 2, 3));

		f = new Format("%3d %d %d");
		assertEquals(" -1 2 3", f.format(-1, 2, 3));

		f = new Format("%3.2f %d %d");
		assertEquals(" -1.00 2 3", f.format(-1, 2, 3));
	}

	@Test
	public void testFormat2() {
//		new Format("%.2fV %.2A)");
		new Format("[%d]W ([%.2f]V @ [%.2f]A)");

	}
}
