package com.chaean.promptdrive.member.internal.adapter.oauth;

import com.chaean.promptdrive.member.internal.dto.SocialIdentityProfileResponse;
import com.chaean.promptdrive.member.internal.domain.SocialProvider;

public interface OAuthProviderClient {

	SocialProvider provider();

	String createAuthorizationUri(String state, String codeChallenge, String nonce);

	SocialIdentityProfileResponse authenticateUser(String authorizationCode, String pkceVerifier, String nonce);
}
