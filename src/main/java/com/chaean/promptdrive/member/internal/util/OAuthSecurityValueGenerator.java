package com.chaean.promptdrive.member.internal.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;

import org.springframework.stereotype.Component;

@Component
public class OAuthSecurityValueGenerator {

	private final SecureRandom secureRandom = new SecureRandom();

	public String generateSecureValue() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public String hashWithSha256(String value) {
		return HexFormat.of().formatHex(calculateSha256Digest(value));
	}

	public String createPkceCodeChallenge(String verifier) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(calculateSha256Digest(verifier));
	}

	private byte[] calculateSha256Digest(String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException exception) {
			throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
		}
	}
}
