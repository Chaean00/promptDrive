package com.chaean.promptdrive.member.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.chaean.promptdrive.common.config.JwtProperties;
import com.chaean.promptdrive.member.internal.persistence.Member;
import com.chaean.promptdrive.member.internal.persistence.RefreshToken;
import com.chaean.promptdrive.member.internal.persistence.RefreshTokenRepository;
import com.chaean.promptdrive.member.internal.util.OAuthSecurityValueGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Refresh token 서비스")
class RefreshTokenManagementServiceTest {

	private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
	private final JwtAccessTokenIssuer accessTokenIssuer = mock(JwtAccessTokenIssuer.class);
	private final JwtProperties properties = new JwtProperties();
	private final OAuthSecurityValueGenerator valueGenerator = mock(OAuthSecurityValueGenerator.class);
	private final Member member = mock(Member.class);
	private RefreshTokenManagementService service;

	@BeforeEach
	void setUp() {
		properties.setRefreshTokenTtl(Duration.ofDays(30));
		given(valueGenerator.hashWithSha256(any(String.class))).willAnswer(invocation -> "hash-" + invocation.getArgument(0));
		given(accessTokenIssuer.issueAccessToken(any(Member.class), any(Instant.class))).willReturn("access-token");
		service = new RefreshTokenManagementService(repository, accessTokenIssuer, properties, valueGenerator);
	}

	@Test
	@DisplayName("새로운 family와 해시된 refresh token을 발급한다")
	void issuesHashedRefreshTokenWithNewFamily() {
		var response = service.issueRefreshToken(member);

		assertThat(response.getAccessToken()).isEqualTo("access-token");
		assertThat(response.getRefreshToken()).isNotBlank();
		assertThat(response.getRefreshTokenTtl()).isEqualTo(Duration.ofDays(30));
		org.mockito.ArgumentCaptor<RefreshToken> captor = org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getTokenHash()).isEqualTo("hash-" + response.getRefreshToken());
		assertThat(captor.getValue().getFamilyId()).isNotNull();
	}

	@Test
	@DisplayName("활성 refresh token을 회전시키고 다음 token을 family에 연결한다")
	void rotatesActiveTokenAndLinksTheNextTokenToThePredecessor() {
		UUID familyId = UUID.randomUUID();
		RefreshToken current = RefreshToken.issue(member, familyId, "hash-raw-token", Instant.now().plusSeconds(300));
		given(repository.findByTokenHash("hash-raw-token")).willReturn(java.util.Optional.of(current));

		var response = service.rotateRefreshToken("raw-token");

		assertThat(response).isNotNull();
		assertThat(current.getRevokedAt()).isNotNull();
		org.mockito.ArgumentCaptor<RefreshToken> captor = org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getFamilyId()).isEqualTo(familyId);
		assertThat(captor.getValue().getPredecessorId()).isNull();
	}

	@Test
	@DisplayName("재사용된 refresh token을 표시하고 family 전체를 폐기한다")
	void marksReusedTokenAndRevokesTheWholeFamily() {
		UUID familyId = UUID.randomUUID();
		RefreshToken reused = RefreshToken.issue(member, familyId, "hash-old", Instant.now().plusSeconds(300));
		reused.revokeRefreshToken(Instant.now());
		RefreshToken sibling = RefreshToken.issue(member, familyId, "hash-sibling", Instant.now().plusSeconds(300));
		given(repository.findByTokenHash("hash-old")).willReturn(java.util.Optional.of(reused));
		given(repository.findAllByFamilyIdAndRevokedAtIsNull(familyId)).willReturn(List.of(sibling));

		assertThat(service.rotateRefreshToken("old")).isNull();

		assertThat(reused.getReusedAt()).isNotNull();
		assertThat(sibling.getRevokedAt()).isNotNull();
	}

	@Test
	@DisplayName("로그아웃 시 family의 모든 활성 token을 폐기한다")
	void revokesAllActiveTokensOnLogout() {
		UUID familyId = UUID.randomUUID();
		RefreshToken current = RefreshToken.issue(member, familyId, "hash-current", Instant.now().plusSeconds(300));
		RefreshToken sibling = RefreshToken.issue(member, familyId, "hash-sibling", Instant.now().plusSeconds(300));
		given(repository.findByTokenHash("hash-current")).willReturn(java.util.Optional.of(current));
		given(repository.findAllByFamilyIdAndRevokedAtIsNull(familyId)).willReturn(List.of(current, sibling));

		service.revokeRefreshToken("current");

		assertThat(current.getRevokedAt()).isNotNull();
		assertThat(sibling.getRevokedAt()).isNotNull();
	}
}
