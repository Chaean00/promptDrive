package com.chaean.promptdrive.member.internal.adapter.oauth;

import com.chaean.promptdrive.member.internal.application.SocialIdentityProfile;
import com.chaean.promptdrive.member.internal.domain.SocialProvider;

public interface OAuthProviderClient {

	SocialProvider provider();

	String authorizationUri(String state, String codeChallenge, String nonce);

	SocialIdentityProfile authenticate(String authorizationCode, String pkceVerifier, String nonce);
}
