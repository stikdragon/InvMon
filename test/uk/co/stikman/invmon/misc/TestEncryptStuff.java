package uk.co.stikman.invmon.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import uk.co.stikman.invmon.remote.EncryptHelper;

public class TestEncryptStuff {
	@Test
	public void testEncryption() {
		EncryptHelper x = new EncryptHelper("12345678");
		byte[] enc = x.encryptUTF8("HELLO");
		String res = x.decryptUTF8(enc, 0, enc.length);
		assertEquals("HELLO", res);

	}
}
