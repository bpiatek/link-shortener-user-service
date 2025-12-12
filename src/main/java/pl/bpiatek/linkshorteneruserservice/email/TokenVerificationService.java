package pl.bpiatek.linkshorteneruserservice.email;

import com.google.common.hash.Hashing;
import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshorteneruserservice.exception.ExpiredTokenException;
import pl.bpiatek.linkshorteneruserservice.exception.InvalidTokenException;

import java.time.Clock;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.SECONDS;

class TokenVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final Clock clock;
    private final long verificationTokenExpirationSec;

    TokenVerificationService(EmailVerificationRepository emailVerificationRepository,
                             Clock clock,
                             long verificationTokenExpirationSec) {
        this.emailVerificationRepository = emailVerificationRepository;
        this.clock = clock;
        this.verificationTokenExpirationSec = verificationTokenExpirationSec;
    }

    String generateAndSaveToken(Long userId, String email) {
        var rawToken = UUID.nameUUIDFromBytes(email.getBytes(UTF_8)).toString();
        var tokenHash = Hashing.sha256()
                .hashString(rawToken, UTF_8)
                .toString();

        var expiresAt = clock.instant().plus(verificationTokenExpirationSec, SECONDS);

        emailVerificationRepository.save(userId, tokenHash, expiresAt);
        return rawToken;
    }

    @Transactional
    Long verifyToken(String rawToken) {
        var tokenHash = Hashing.sha256()
                .hashString(rawToken, UTF_8)
                .toString();

        var verification = emailVerificationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token."));

        if (verification.expiresAt().isBefore(clock.instant())) {
            throw new ExpiredTokenException("Verification token has expired.");
        }

        emailVerificationRepository.deleteByUserId(verification.userId());

        return verification.userId();
    }
}
