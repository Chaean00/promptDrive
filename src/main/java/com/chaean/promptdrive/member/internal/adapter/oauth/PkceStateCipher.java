package com.chaean.promptdrive.member.internal.adapter.oauth;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class PkceStateCipher {

	private static final int IV_LENGTH = 12;
	private static final int TAG_LENGTH = 128;

	private final MemberOAuthProperties properties;
	private final SecureRandom secureRandom = new SecureRandom();

	public PkceStateCipher(MemberOAuthProperties properties) {
		this.properties = properties;
	}

	public String encrypt(String value) {
		try {
			byte[] iv = new byte[IV_LENGTH];
			secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH, iv));
			byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
					.put(iv).put(encrypted).array());
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Unable to encrypt OAuth state", exception);
		}
	}

	public String decrypt(String value) {
		try {
			byte[] payload = Base64.getUrlDecoder().decode(value);
			if (payload.length <= IV_LENGTH) {
				throw new IllegalArgumentException("Invalid encrypted OAuth state");
			}
			byte[] iv = new byte[IV_LENGTH];
			byte[] encrypted = new byte[payload.length - IV_LENGTH];
			System.arraycopy(payload, 0, iv, 0, iv.length);
			System.arraycopy(payload, iv.length, encrypted, 0, encrypted.length);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException | IllegalArgumentException exception) {
			throw new IllegalStateException("Unable to decrypt OAuth state", exception);
		}
	}

	private SecretKey key() {
		properties.requireStateEncryptionKey();
		byte[] key = Base64.getDecoder().decode(properties.getStateEncryptionKey());
		if (key.length != 32) {
			throw new IllegalStateException("member.oauth.state-encryption-key must be a base64 encoded 32-byte key");
		}
		return new SecretKeySpec(key, "AES");
	}
}
