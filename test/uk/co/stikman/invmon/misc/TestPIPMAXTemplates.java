package uk.co.stikman.invmon.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import uk.co.stikman.invmon.inverter.Template;
import uk.co.stikman.invmon.inverter.TemplateResult;

public class TestPIPMAXTemplates {
	@Test
	public void testTemplate1() {
		Template t = new Template("<AA.A> <BBB.B> <C+>");
		TemplateResult r = t.apply("12.3 456.7 9999");
		assertEquals("12.3", r.getString("A"));
		assertEquals("456.7", r.getString("B"));
		assertEquals("9999", r.getString("C"));

		r = t.apply("12.3 456.7 9");
		assertEquals("9", r.getString("C"));

		r = t.apply("12.3 456.7 999999");
		assertEquals("999999", r.getString("C"));
	}

	@Test
	public void testRedsQPIGS() {
		// 2023-04-15 09:03:42   INFO [*.inverter.PIP8048MAX.PIP8048MAX] QPIGS = 000.0 00.0 229.8 50.0 00345 00286 003 371 53.00 000 093 0030 00.0 000.0 00.00 00005 00010000 00 00 00000 010
		Template t = new Template("<BBB.B> <CC.C> <DDD.D> <EE.E> <F+> <G+> <HHH> <III> <JJ.JJ> <KKK> <OOO> <TTTT> <ee.e> <UUU.U> <WW.WW> <PPPPP> <AAAAAAAA> <QQ> <VV> <MMMMM> <ZZZ>");
		TemplateResult r = t.apply("000.0 00.0 229.8 50.0 00345 00286 003 371 53.00 000 093 0030 00.0 000.0 00.00 00005 00010000 00 00 00000 010");
		assertEquals(93, r.getInteger("O"));
		
		System.out.println(r);

	}
}
