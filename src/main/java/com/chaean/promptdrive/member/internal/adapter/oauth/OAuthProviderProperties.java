package com.chaean.promptdrive.member.internal.adapter.oauth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OAuthProviderProperties {

	private String clientId;
	private String clientSecret;
	private String redirectUri;
	private String authorizationUri;
	private String tokenUri;
	private String userInfoUri;

	public OAuthProviderProperties(String authorizationUri, String tokenUri, String userInfoUri) {
		this.authorizationUri = authorizationUri;
		this.tokenUri = tokenUri;
		this.userInfoUri = userInfoUri;
	}
}
