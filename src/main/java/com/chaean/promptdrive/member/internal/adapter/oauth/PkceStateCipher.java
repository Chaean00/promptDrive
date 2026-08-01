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

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PkceStateCipher {

	private static final int IV_LENGTH = 12;
	private static final int TAG_LENGTH = 128;

	private final MemberOAuthProperties properties;
	private final SecureRandom secureRandom = new SecureRandom();

	public String encryptPkceVerifier(String verifier) {
		try {
			byte[] iv = new byte[IV_LENGTH];
			secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, createStateEncryptionKey(), new GCMParameterSpec(TAG_LENGTH, iv));
			byte[] encrypted = cipher.doFinal(verifier.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
					.put(iv).put(encrypted).array());
		} catch (GeneralSecurityException exception) {
			throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	public String decryptPkceVerifier(String encryptedVerifier) {
		try {
			byte[] payload = decodeEncryptedPayload(encryptedVerifier);
			if (payload.length <= IV_LENGTH) {
				throw new BusinessException(CommonErrorCode.UNAUTHORIZED_REQUEST);
			}
			byte[] iv = new byte[IV_LENGTH];
			byte[] encrypted = new byte[payload.length - IV_LENGTH];
			System.arraycopy(payload, 0, iv, 0, iv.length);
			System.arraycopy(payload, iv.length, encrypted, 0, encrypted.length);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, createStateEncryptionKey(), new GCMParameterSpec(TAG_LENGTH, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException exception) {
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED_REQUEST);
		}
	}

	private SecretKey createStateEncryptionKey() {
		try {
			properties.requireStateEncryptionKey();
			byte[] key = Base64.getDecoder().decode(properties.getStateEncryptionKey());
			if (key.length != 32) {
				throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE);
			}
			return new SecretKeySpec(key, "AES");
		} catch (BusinessException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE);
		}
	}

	private byte[] decodeEncryptedPayload(String encryptedValue) {
		try {
			return Base64.getUrlDecoder().decode(encryptedValue);
		} catch (RuntimeException exception) {
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED_REQUEST);
		}
	}
}
