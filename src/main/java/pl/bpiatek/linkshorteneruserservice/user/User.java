package pl.bpiatek.linkshorteneruserservice.user;

import java.time.Instant;

record User(
        Long id,
        String email,
        String passwordHash,
        String role,
        boolean isEmailVerified,
        Instant createdAt
) {}