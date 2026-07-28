package com.chaean.promptdrive.common.config;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;

import org.springframework.stereotype.Component;

@Component
public class JwtSigningKeyFactory {

	public SecretKey createSigningKey(String encodedSigningKey) {
		try {
			byte[] key = Base64.getDecoder().decode(encodedSigningKey);
			if (key.length != 32) {
				throw new BusinessException(CommonErrorCode.INVALID_SECURITY_CONFIGURATION);
			}
			return new SecretKeySpec(key, "HmacSHA256");
		} catch (BusinessException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw new BusinessException(CommonErrorCode.INVALID_SECURITY_CONFIGURATION);
		}
	}
}
