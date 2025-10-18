package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.SECONDS;

class VerificationTokenService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final long verificationTokenExpirationSec;

    VerificationTokenService(EmailVerificationRepository emailVerificationRepository,
                             PasswordEncoder passwordEncoder,
                             Clock clock,
                             long verificationTokenExpirationSec) {
        this.emailVerificationRepository = emailVerificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.verificationTokenExpirationSec = verificationTokenExpirationSec;
    }

    String generateAndSaveToken(User user) {
        var rawToken = UUID.nameUUIDFromBytes(user.email().getBytes(UTF_8)).toString();
        var tokenHash = passwordEncoder.encode(rawToken);
        var expiresAt = clock.instant().plus(verificationTokenExpirationSec, SECONDS);

        emailVerificationRepository.save(user.id(), tokenHash, expiresAt);
        return rawToken;
    }
}
