package com.chaean.promptdrive.member.internal.adapter.oauth;

import java.util.List;

import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.common.web.error.CommonErrorCode;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "member.oauth")
public class MemberOAuthProperties {

	private OAuthProviderProperties google = new OAuthProviderProperties(
			"https://accounts.google.com/o/oauth2/v2/auth",
			"https://oauth2.googleapis.com/token",
			"https://openidconnect.googleapis.com/v1/userinfo");
	private OAuthProviderProperties kakao = new OAuthProviderProperties(
			"https://kauth.kakao.com/oauth/authorize",
			"https://kauth.kakao.com/oauth/token",
			"https://kapi.kakao.com/v2/user/me");
	private List<String> allowedReturnPaths = List.of("/");
	private List<String> allowedOrigins = List.of();
	private String stateEncryptionKey;

	public void requireConfigured(OAuthProviderProperties provider) {
		if (isBlank(provider.getClientId()) || isBlank(provider.getClientSecret()) || isBlank(provider.getRedirectUri())) {
			throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE);
		}
	}

	public void requireStateEncryptionKey() {
		if (isBlank(stateEncryptionKey)) {
			throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}
