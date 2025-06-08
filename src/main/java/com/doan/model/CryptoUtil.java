package com.doan.model;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CryptoUtil {
	private static final String KEY = "MySecretKey12345"; // 16 chars for AES-128

	public static String encrypt(String data) throws Exception {
		SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(), "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);
		return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
	}

	public static String decrypt(String encrypted) throws Exception {
		SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(), "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, secretKey);
		return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)));
	}
}
