package pl.bpiatek.linkshorteneruserservice.password;

import com.google.common.hash.Hashing;
import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshorteneruserservice.exception.ExpiredTokenException;
import pl.bpiatek.linkshorteneruserservice.exception.InvalidTokenException;

import java.time.Clock;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

class PasswordResetTokenService {

    private final PasswordResetTokenRepository repository;
    private final Clock clock;
    private final long expirationSec;

    PasswordResetTokenService(PasswordResetTokenRepository repository, Clock clock, long expirationSec) {
        this.repository = repository;
        this.clock = clock;
        this.expirationSec = expirationSec;
    }

    String generateAndSaveToken(String userId) {
        var rawToken = UUID.randomUUID().toString();
        var tokenHash = Hashing.sha256().hashString(rawToken, UTF_8).toString();
        var expiresAt = clock.instant().plusSeconds(expirationSec);

        repository.save(Long.valueOf(userId), tokenHash, expiresAt);
        return rawToken;
    }

    @Transactional
    Long validateAndConsumeToken(String rawToken) {
        var tokenHash = Hashing.sha256().hashString(rawToken, UTF_8).toString();

        var token = repository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid reset token."));

        if (token.expiresAt().isBefore(clock.instant())) {
            throw new ExpiredTokenException("Reset token has expired.");
        }

        repository.deleteByUserId(token.userId());

        return token.userId();
    }

    void deleteTokenForUser(Long userId) {
        repository.deleteByUserId(userId);
    }
}
