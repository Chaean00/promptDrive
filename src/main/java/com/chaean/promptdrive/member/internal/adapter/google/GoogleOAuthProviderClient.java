package com.chaean.promptdrive.member.internal.adapter.google;

import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.member.internal.adapter.oauth.MemberOAuthProperties;
import com.chaean.promptdrive.member.internal.adapter.oauth.OAuthProviderClient;
import com.chaean.promptdrive.member.internal.adapter.oauth.OAuthProviderProperties;
import com.chaean.promptdrive.member.internal.dto.GoogleUserInfoResponse;
import com.chaean.promptdrive.member.internal.dto.OAuthTokenResponse;
import com.chaean.promptdrive.member.internal.dto.SocialIdentityProfileResponse;
import com.chaean.promptdrive.member.internal.domain.SocialProvider;
import com.chaean.promptdrive.member.internal.util.OAuthSecurityValueGenerator;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GoogleOAuthProviderClient implements OAuthProviderClient {

	private final RestClient restClient;
	private final MemberOAuthProperties properties;
	private final ObjectProvider<JwtDecoder> idTokenDecoderProvider;
	private final OAuthSecurityValueGenerator valueGenerator;

	public GoogleOAuthProviderClient(RestClient.Builder restClientBuilder, MemberOAuthProperties properties,
			@Qualifier("googleIdTokenDecoder") ObjectProvider<JwtDecoder> idTokenDecoderProvider,
			OAuthSecurityValueGenerator valueGenerator) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
		this.idTokenDecoderProvider = idTokenDecoderProvider;
		this.valueGenerator = valueGenerator;
	}

	@Override
	public SocialProvider provider() {
		return SocialProvider.GOOGLE;
	}

	@Override
	public String authorizationUri(String state, String codeChallenge, String nonce) {
		OAuthProviderProperties provider = properties.getGoogle();
		properties.requireConfigured(provider);
		return UriComponentsBuilder.fromUriString(provider.getAuthorizationUri())
				.queryParam("client_id", provider.getClientId())
				.queryParam("redirect_uri", provider.getRedirectUri())
				.queryParam("response_type", "code")
				.queryParam("scope", "openid profile email")
				.queryParam("state", state)
				.queryParam("nonce", nonce)
				.queryParam("code_challenge", codeChallenge)
				.queryParam("code_challenge_method", "S256")
				.build().encode().toUriString();
	}

	@Override
	public SocialIdentityProfileResponse authenticate(String authorizationCode, String pkceVerifier, String nonceHash) {
		OAuthProviderProperties provider = properties.getGoogle();
		properties.requireConfigured(provider);

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("code", authorizationCode);
		form.add("client_id", provider.getClientId());
		form.add("client_secret", provider.getClientSecret());
		form.add("redirect_uri", provider.getRedirectUri());
		form.add("grant_type", "authorization_code");
		form.add("code_verifier", pkceVerifier);

		OAuthTokenResponse token = requestToken(provider.getTokenUri(), form);
		if (token == null || token.getAccessToken() == null || token.getIdToken() == null) {
			throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR);
		}

		Jwt validatedIdToken = validateIdToken(token.getIdToken(), provider.getClientId(), nonceHash);
		GoogleUserInfoResponse userInfo = restClient.get().uri(provider.getUserInfoUri())
				.headers(headers -> headers.setBearerAuth(token.getAccessToken()))
				.retrieve().onStatus(status -> status.isError(),
						(request, response) -> { throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR); })
				.body(GoogleUserInfoResponse.class);

		if (userInfo == null || userInfo.getSub() == null || userInfo.getSub().isBlank()
				|| !userInfo.getSub().equals(validatedIdToken.getSubject())) {
			throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR);
		}

		return SocialIdentityProfileResponse.of(SocialProvider.GOOGLE, userInfo.getSub(), userInfo.getName(),
				userInfo.isEmailVerified() ? userInfo.getEmail() : null);
	}

	private Jwt validateIdToken(String idToken, String clientId, String nonceHash) {
		try {
			Jwt jwt = idTokenDecoderProvider.getObject().decode(idToken);
			if (!jwt.getAudience().contains(clientId) || !nonceHash.equals(valueGenerator.sha256(jwt.getClaimAsString("nonce")))) {
				throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR);
			}
			return jwt;
		} catch (BusinessException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR);
		}
	}

	private OAuthTokenResponse requestToken(String tokenUri, MultiValueMap<String, String> form) {
		return restClient.post().uri(tokenUri).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
				.retrieve().onStatus(status -> status.isError(),
						(request, response) -> { throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR); })
				.body(OAuthTokenResponse.class);
	}
}
