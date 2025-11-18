package pl.bpiatek.linkshorteneruserservice.email;

import java.time.Instant;

record EmailVerification(
        Long id,
        long userId,
        String tokenHash,
        Instant expiresAt,
        Instant createdAt
) {}