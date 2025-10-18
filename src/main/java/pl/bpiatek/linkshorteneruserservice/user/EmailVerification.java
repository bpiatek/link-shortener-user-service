package pl.bpiatek.linkshorteneruserservice.user;

import java.time.Instant;

record EmailVerification(
        Long id,
        long userId,
        String tokenHash,
        Instant expiresAt,
        Instant createdAt
) {}