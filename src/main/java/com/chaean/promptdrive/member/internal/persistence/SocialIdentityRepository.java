package com.chaean.promptdrive.member.internal.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialIdentityRepository extends JpaRepository<SocialIdentity, Long> {

	Optional<SocialIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);
}
