package pl.bpiatek.linkshorteneruserservice.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshorteneruserservice.exception.InvalidRefreshTokenException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private final RefreshTokenStore refreshTokenStore;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenStore refreshTokenStore, Clock clock) {
        this.refreshTokenStore = refreshTokenStore;
        this.clock = clock;
    }

    @Transactional
    void saveRefreshToken(long userId, String rawToken, Instant expiresAt) {
        log.info("Saving refresh token for user with ID: {}, rawToken: {}", userId, rawToken);
        var hash = hashToken(rawToken);
        log.info("Saving refresh tokenHash for user with ID: {}, tokenHash: {}", userId, hash);
        var token = new RefreshToken(null, userId, hash, expiresAt, clock.instant());
        refreshTokenStore.save(token);
    }

    @Transactional
    long validateAndRotate(String rawToken) {
        var hash = hashToken(rawToken);
        log.info("Refreshing user with refresh tokenHash: {}", hash);
        var tokenEntity = refreshTokenStore.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found in database"));

        if (tokenEntity.expiresAt().isBefore(clock.instant())) {
            refreshTokenStore.deleteById(tokenEntity.id());
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        refreshTokenStore.deleteById(tokenEntity.id());

        return tokenEntity.userId();
    }

    @Transactional
    public void revokeToken(String rawRefreshToken) {
        var hash = hashToken(rawRefreshToken);

        var optionalRefreshToken = refreshTokenStore.findByTokenHash(hash);
        if (optionalRefreshToken.isEmpty()) {
            log.info("Refresh token: {} not found in database by tokenHash: {}", rawRefreshToken, hash);
        }

        optionalRefreshToken
                .ifPresent(token -> refreshTokenStore.deleteById(token.id()));
    }

    @Transactional
    public void revokeAllTokensForUser(long userId) {
        refreshTokenStore.deleteAllByUserId(userId);
    }

    String hashToken(String rawToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
