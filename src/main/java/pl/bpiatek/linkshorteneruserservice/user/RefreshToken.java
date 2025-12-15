package pl.bpiatek.linkshorteneruserservice.user;

import java.time.Instant;

record RefreshToken(
        Long id,
        long userId,
        String tokenHash,
        Instant expiresAt,
        Instant createdAt
) {}