package uk.co.stikman.invmon.remote;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptHelper {
	private SecretKeySpec	key;
	private IvParameterSpec	initvec;
	private Cipher			cipher;

	public EncryptHelper(String pass) {
		try {
			String salt = "12345678";
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec ks = new PBEKeySpec(pass.toCharArray(), salt.getBytes(), 65536, 256);
			key = new SecretKeySpec(skf.generateSecret(ks).getEncoded(), "AES");
			byte[] iv = new byte[16];
			new SecureRandom().nextBytes(iv);
			initvec = new IvParameterSpec(iv);
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		} catch (Exception e) {
			throw new RuntimeException("Could not create key");
		}
	}

	public byte[] encryptUTF8(String data) {
		byte[] x = data.getBytes(StandardCharsets.UTF_8);
		return encrypt(x, 0, x.length);
	}

	public byte[] encrypt(byte[] data, int offset, int length) {
		try {
			cipher.init(Cipher.ENCRYPT_MODE, key, initvec);
			return cipher.doFinal(data, offset, length);
		} catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
			throw new RuntimeException("Failed to encrypt: " + e.getMessage(), e);
		}
	}

	public String decryptUTF8(byte[] data, int offset, int length) {
		byte[] arr = decrypt(data, offset, length);
		return new String(arr, StandardCharsets.UTF_8);
	}

	public byte[] decrypt(byte[] data, int offset, int length) {
		try {
			cipher.init(Cipher.DECRYPT_MODE, key, initvec);
			return cipher.doFinal(data, offset, length);
		} catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
			throw new RuntimeException("Failed to decrypt: " + e.getMessage(), e);
		}
	}

}
