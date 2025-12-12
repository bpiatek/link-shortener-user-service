package pl.bpiatek.linkshorteneruserservice.email;

import com.google.common.hash.Hashing;

import java.time.Instant;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestEmailVerification {
    private final Long id;
    private final long userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private final Instant createdAt;

    private TestEmailVerification(TestEmailVerificationBuilder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.tokenHash = builder.tokenHash;
        this.expiresAt = builder.expiresAt;
        this.createdAt = builder.createdAt;
    }

    public static TestEmailVerificationBuilder builder() {
        return new TestEmailVerificationBuilder();

    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static class TestEmailVerificationBuilder {
        private Long id;
        private long userId;
        private String tokenHash = hashToken("default_test_token");
        private Instant expiresAt = Instant.parse("2025-01-01T12:00:00Z");
        private Instant createdAt = Instant.parse("2025-01-01T10:00:00Z");

        private String hashToken(String rawToken) {
            return Hashing.sha256()
                    .hashString(rawToken, UTF_8)
                    .toString();
        }

        public TestEmailVerificationBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public TestEmailVerificationBuilder userId(long userId) {
            this.userId = userId;
            return this;
        }

        public TestEmailVerificationBuilder rawToken(String rawToken) {
            this.tokenHash = hashToken(rawToken);
            return this;
        }

        public TestEmailVerificationBuilder tokenHash(String tokenHash) {
            this.tokenHash = tokenHash;
            return this;
        }

        public TestEmailVerificationBuilder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public TestEmailVerificationBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public TestEmailVerification build() {
            return new TestEmailVerification(this);
        }
    }
}

