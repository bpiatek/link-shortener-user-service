package pl.bpiatek.linkshorteneruserservice.password;

import java.time.Instant;
import java.util.Optional;

interface PasswordResetTokenRepository {
    void save(Long userId, String tokenHash, Instant expiresAt);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteByUserId(long userId);
}
