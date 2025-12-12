package pl.bpiatek.linkshorteneruserservice.password;

import java.time.Instant;

record PasswordResetToken(
        Long id,
        long userId,
        String tokenHash,
        Instant expiresAt,
        Instant createdAt) {
}
