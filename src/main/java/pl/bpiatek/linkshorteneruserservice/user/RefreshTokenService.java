package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshorteneruserservice.exception.InvalidRefreshTokenException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;

class RefreshTokenService {

    private final RefreshTokenStore refreshTokenStore;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenStore refreshTokenStore, Clock clock) {
        this.refreshTokenStore = refreshTokenStore;
        this.clock = clock;
    }

    @Transactional
    void saveRefreshToken(long userId, String rawToken, Instant expiresAt) {
        var hash = hashToken(rawToken);
        var token = new RefreshToken(null, userId, hash, expiresAt, clock.instant());
        refreshTokenStore.save(token);
    }

    @Transactional
    long validateAndRotate(String rawToken) {
        var hash = hashToken(rawToken);

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
