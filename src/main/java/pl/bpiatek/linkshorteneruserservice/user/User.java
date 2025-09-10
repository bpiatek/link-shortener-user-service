package pl.bpiatek.linkshorteneruserservice.user;

import java.time.Instant;
import java.util.List;

record User(
        Long id,
        String email,
        String passwordHash,
        List<String> roles,
        boolean isEmailVerified,
        Instant createdAt
) {}