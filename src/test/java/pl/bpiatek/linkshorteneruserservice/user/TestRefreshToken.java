package pl.bpiatek.linkshorteneruserservice.user;

import com.google.common.hash.Hashing;
import org.assertj.core.api.AbstractStringAssert;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRefreshToken {

    private final Long id;
    private final Long userId;
    private final String tokenHash;
    private final java.time.Instant expiresAt;
    private final java.time.Instant createdAt;

    TestRefreshToken(TestRefreshTokenBuilder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.tokenHash = builder.tokenHash;
        this.expiresAt = builder.expiresAt;
        this.createdAt = builder.createdAt;
    }

    static TestRefreshTokenBuilder builder(Long userId) {
        return new TestRefreshTokenBuilder(userId);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public java.time.Instant getExpiresAt() {
        return expiresAt;
    }

    public java.time.Instant getCreatedAt() {
        return createdAt;
    }

    public static class TestRefreshTokenBuilder {
        private Long id;
        private final Long userId;
        private String tokenHash = "hashed_refresh_token_123";
        private java.time.Instant expiresAt = java.time.Instant.parse("2025-01-01T11:00:00Z");
        private java.time.Instant createdAt = java.time.Instant.parse("2025-01-01T10:00:00Z");

        private TestRefreshTokenBuilder(Long userId) {
            this.userId = userId;
        }

        public TestRefreshTokenBuilder id(Long id) {
            this.id = id;
            return this;
        }

        TestRefreshTokenBuilder hashTokenFromRaw(String rawToken) {
            this.tokenHash = Hashing.sha256()
                    .hashString(rawToken, StandardCharsets.UTF_8)
                    .toString();
            return this;
        }

        public TestRefreshTokenBuilder tokenHash(String tokenHash) {
            this.tokenHash = tokenHash;
            return this;
        }

        public TestRefreshTokenBuilder expiresAt(java.time.Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public TestRefreshTokenBuilder createdAt(java.time.Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public TestRefreshToken build() {
            return new TestRefreshToken(this);
        }
    }
}
