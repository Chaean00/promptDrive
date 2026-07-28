package com.chaean.promptdrive.member.internal.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.member.internal.application.ConsumeOAuthLoginAttemptService;
import com.chaean.promptdrive.member.internal.application.RefreshTokenManagementService;
import com.chaean.promptdrive.member.internal.domain.MemberRole;
import com.chaean.promptdrive.member.internal.domain.SocialProvider;
import com.chaean.promptdrive.member.internal.dto.TokenPairResponse;
import com.chaean.promptdrive.member.internal.util.OAuthSecurityValueGenerator;

@SpringBootTest(properties = {
		"spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate",
		"security.jwt.signing-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
		"security.jwt.issuer=promptdrive",
		"security.jwt.audience=promptdrive-api"
})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Member OAuth MySQL 통합 테스트")
class MemberOAuthMySqlIntegrationTest {

	@Container
	@ServiceConnection
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

	@Autowired
	private OAuthLoginAttemptRepository loginAttemptRepository;

	@Autowired
	private ConsumeOAuthLoginAttemptService consumeOAuthLoginAttemptService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private RefreshTokenManagementService refreshTokenManagementService;

	@Autowired
	private OAuthSecurityValueGenerator valueGenerator;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("OAuth state를 저장하고 한 번만 소비한다")
	void persistsAndConsumesOAuthStateOnlyOnce() {
		String stateHash = uniqueValue();
		loginAttemptRepository.saveAndFlush(new OAuthLoginAttempt(
				SocialProvider.GOOGLE, stateHash, "encrypted-verifier", "nonce-hash", "/",
				Instant.now().plusSeconds(300)));

		OAuthLoginAttempt consumed = consumeOAuthLoginAttemptService.consumeOAuthLoginAttempt(SocialProvider.GOOGLE, stateHash);

		assertThat(consumed.getConsumedAt()).isNotNull();
		assertThatThrownBy(() -> consumeOAuthLoginAttemptService.consumeOAuthLoginAttempt(SocialProvider.GOOGLE, stateHash))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(CommonErrorCode.UNAUTHORIZED_REQUEST);
	}

	@Test
	@DisplayName("refresh token을 해시로 저장하고 재사용 시 token family 전체를 폐기한다")
	void persistsHashedRefreshTokensAndRevokesTheTokenFamilyOnReuse() {
		Member member = memberRepository.saveAndFlush(new Member("oauth-integration-" + uniqueValue(), MemberRole.MEMBER));

		TokenPairResponse first = refreshTokenManagementService.issueRefreshToken(member);
		String firstHash = valueGenerator.hashWithSha256(first.getRefreshToken());

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from refresh_token where token_hash = ?", Integer.class, firstHash)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from refresh_token where token_hash = ?", Integer.class, first.getRefreshToken())).isZero();

		TokenPairResponse next = refreshTokenManagementService.rotateRefreshToken(first.getRefreshToken());
		assertThat(next).isNotNull();

		String familyId = jdbcTemplate.queryForObject(
				"select family_id from refresh_token where token_hash = ?", String.class, firstHash);
		assertThat(jdbcTemplate.queryForObject(
				"select case when revoked_at is null then 0 else 1 end from refresh_token where token_hash = ?",
				Integer.class, firstHash)).isEqualTo(1);

		assertThat(refreshTokenManagementService.rotateRefreshToken(first.getRefreshToken())).isNull();
		assertThat(jdbcTemplate.queryForObject(
				"select case when reused_at is null then 0 else 1 end from refresh_token where token_hash = ?",
				Integer.class, firstHash)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from refresh_token where family_id = ? and revoked_at is null", Integer.class, familyId))
				.isZero();
	}

	private String uniqueValue() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
