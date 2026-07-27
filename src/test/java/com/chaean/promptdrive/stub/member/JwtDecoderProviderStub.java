package com.chaean.promptdrive.stub.member;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.jwt.JwtDecoder;

public class JwtDecoderProviderStub implements ObjectProvider<JwtDecoder> {

	private JwtDecoder decoder;

	public void set(JwtDecoder decoder) {
		this.decoder = decoder;
	}

	@Override
	public JwtDecoder getObject() {
		return decoder;
	}
}
