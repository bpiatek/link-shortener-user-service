package pl.bpiatek.linkshorteneruserservice.password;

import java.time.Instant;

public record PasswordResetToken(
        Long id,
        long userId,
        String tokenHash,
        Instant expiresAt,
        Instant createdAt) {
}
