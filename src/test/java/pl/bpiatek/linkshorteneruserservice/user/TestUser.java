package pl.bpiatek.linkshorteneruserservice.user;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TestUser {
    private final Long id;
    private final String email;
    private final String passwordHash;
    private final List<String> roles;
    private final boolean isEmailVerified;
    private final Instant createdAt;

    TestUser(TestUserBuilder builder) {
        this.id = builder.id;
        this.email = builder.email;
        this.passwordHash = builder.passwordHash;
        this.roles = builder.getRoles();
        this.isEmailVerified = builder.isEmailVerified;
        this.createdAt = builder.createdAt;
    }

    static  TestUserBuilder builder() {
        return  new TestUserBuilder();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public List<String> getRoles() {
        return roles;
    }

    public boolean isEmailVerified() {
        return isEmailVerified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static class TestUserBuilder {
        private Long id;
        private String email = "test@test.com";
        private String passwordHash = "hashed_password_123";
        private final Set<String> roles = new LinkedHashSet<>(Set.of("ROLE_USER"));
        private boolean isEmailVerified;
        private Instant createdAt = Instant.parse("2025-01-01T10:00:00Z");

        public TestUserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public TestUserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public  TestUserBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public TestUserBuilder roles(List<String> additionalRoles) {
            this.roles.addAll(additionalRoles);
            return this;
        }

        public TestUserBuilder isEmailVerified(boolean isEmailVerified) {
            this.isEmailVerified = isEmailVerified;
            return this;
        }

        public TestUserBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public TestUser build() {
            return new TestUser(this);
        }

        public List<String> getRoles() {
            return new ArrayList<>(this.roles);
        }
    }
}
