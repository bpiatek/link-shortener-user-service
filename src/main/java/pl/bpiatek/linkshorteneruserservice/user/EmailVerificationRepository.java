package pl.bpiatek.linkshorteneruserservice.user;

import java.time.Instant;
import java.util.Optional;

interface EmailVerificationRepository {

    void save(long userId, String tokenHash, Instant expiresAt);

    Optional<EmailVerification> findByTokenHash(String tokenHash);

    void deleteByUserId(long userId);
}
