package uk.co.stikman.invmon.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import uk.co.stikman.invmon.inverter.Template;
import uk.co.stikman.invmon.inverter.TemplateResult;

public class TestPIPMAXTemplates {
	@Test
	public void testTemplate1() {
		Template t = new Template("<AA.A> <BBB.B> <C+>");
		TemplateResult r =  t.apply("12.3 456.7 9999");
		assertEquals("12.3", r.getString("A"));
		assertEquals("456.7", r.getString("B"));
		assertEquals("9999", r.getString("C"));
		
		r =  t.apply("12.3 456.7 9");
		assertEquals("9", r.getString("C"));
		
		r =  t.apply("12.3 456.7 999999");
		assertEquals("999999", r.getString("C"));
	}
}
