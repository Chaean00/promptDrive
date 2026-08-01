package com.chaean.promptdrive.member.internal.adapter.kakao;

import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.member.internal.adapter.oauth.MemberOAuthProperties;
import com.chaean.promptdrive.member.internal.adapter.oauth.OAuthProviderClient;
import com.chaean.promptdrive.member.internal.adapter.oauth.OAuthProviderProperties;
import com.chaean.promptdrive.member.internal.dto.KakaoAccountResponse;
import com.chaean.promptdrive.member.internal.dto.KakaoUserInfoResponse;
import com.chaean.promptdrive.member.internal.dto.OAuthTokenResponse;
import com.chaean.promptdrive.member.internal.dto.SocialIdentityProfileResponse;
import com.chaean.promptdrive.member.internal.domain.SocialProvider;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class KakaoOAuthProviderClient implements OAuthProviderClient {

	private final RestClient restClient;
	private final MemberOAuthProperties properties;

	public KakaoOAuthProviderClient(RestClient.Builder restClientBuilder, MemberOAuthProperties properties) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
	}

	@Override
	public SocialProvider provider() {
		return SocialProvider.KAKAO;
	}

	@Override
	public String createAuthorizationUri(String state, String codeChallenge, String nonce) {
		OAuthProviderProperties provider = properties.getKakao();
		properties.requireConfigured(provider);
		return UriComponentsBuilder.fromUriString(provider.getAuthorizationUri())
				.queryParam("client_id", provider.getClientId())
				.queryParam("redirect_uri", provider.getRedirectUri())
				.queryParam("response_type", "code")
				.queryParam("scope", "profile_nickname")
				.queryParam("state", state)
				.queryParam("code_challenge", codeChallenge)
				.queryParam("code_challenge_method", "S256")
				.build().encode().toUriString();
	}

	@Override
	public SocialIdentityProfileResponse authenticateUser(String authorizationCode, String pkceVerifier, String nonce) {
		OAuthProviderProperties provider = properties.getKakao();
		properties.requireConfigured(provider);
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("code", authorizationCode);
		form.add("client_id", provider.getClientId());
		form.add("client_secret", provider.getClientSecret());
		form.add("redirect_uri", provider.getRedirectUri());
		form.add("grant_type", "authorization_code");
		form.add("code_verifier", pkceVerifier);
		OAuthTokenResponse token = restClient.post().uri(provider.getTokenUri()).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form).retrieve().onStatus(status -> status.isError(),
						(request, response) -> { throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR); })
				.body(OAuthTokenResponse.class);
		if (token == null || token.getAccessToken() == null) {
			throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR);
		}
		KakaoUserInfoResponse userInfo = restClient.get().uri(provider.getUserInfoUri())
				.headers(headers -> headers.setBearerAuth(token.getAccessToken())).retrieve()
				.onStatus(status -> status.isError(),
						(request, response) -> { throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR); })
				.body(KakaoUserInfoResponse.class);
		if (userInfo == null || userInfo.getId() == null || userInfo.getId().isBlank()) {
			throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR);
		}
		KakaoAccountResponse account = userInfo.getKakaoAccount();
		String nickname = account == null || account.getProfile() == null ? null : account.getProfile().getNickname();
		String email = account != null && account.isEmailValid() && account.isEmailVerified()
				? account.getEmail() : null;
		return SocialIdentityProfileResponse.of(SocialProvider.KAKAO, userInfo.getId(), nickname, email);
	}
}
