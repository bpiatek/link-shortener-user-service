package pl.bpiatek.linkshorteneruserservice.password;

import java.time.Instant;

public class TestPasswordResetToken {
    private final Long id;
    private final long userId;
    private final String tokenHash;
    private final String token;
    private final Instant expiresAt;
    private final Instant createdAt;

    private TestPasswordResetToken(TestPasswordResetTokenBuilder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.token = builder.token;
        this.tokenHash = builder.tokenHash;
        this.expiresAt = builder.expiresAt;
        this.createdAt = builder.createdAt;
    }

    public static TestPasswordResetTokenBuilder builder() {
        return new TestPasswordResetTokenBuilder();
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
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

    public static class TestPasswordResetTokenBuilder {
        private Long id;
        private long userId = 1;
        private String token = "test_token";
        private String tokenHash = "test_token_hash";
        private Instant expiresAt = Instant.parse("2025-01-01T12:00:00Z");
        private Instant createdAt = Instant.parse("2025-01-01T10:00:00Z");

        public TestPasswordResetTokenBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public TestPasswordResetTokenBuilder userId(long userId) {
            this.userId = userId;
            return this;
        }

        public  TestPasswordResetTokenBuilder token(String token) {
            this.token = token;
            return this;
        }

        public TestPasswordResetTokenBuilder tokenHash(String tokenHash) {
            this.tokenHash = tokenHash;
            return this;
        }

        public TestPasswordResetTokenBuilder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public TestPasswordResetTokenBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public TestPasswordResetToken build() {
            return new TestPasswordResetToken(this);
        }
    }
}
