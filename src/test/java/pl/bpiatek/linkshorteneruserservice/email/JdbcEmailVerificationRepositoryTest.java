package pl.bpiatek.linkshorteneruserservice.email;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import pl.bpiatek.linkshorteneruserservice.WithPostgres;
import pl.bpiatek.linkshorteneruserservice.user.RoleCacheProvider;
import pl.bpiatek.linkshorteneruserservice.user.UserFixtures;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@JdbcTest
@Import({JdbcEmailVerificationRepository.class, EmailVerificationFixtures.class, UserFixtures.class, RoleCacheProvider.class})
@ActiveProfiles("test")
class JdbcEmailVerificationRepositoryTest implements WithPostgres {

    @Autowired
    private JdbcEmailVerificationRepository repository;

    @Autowired
    private EmailVerificationFixtures emailFixtures;

    @Autowired
    private UserFixtures userFixtures;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Instant NOW = Instant.parse("2024-01-01T12:00:00Z");

    @TestConfiguration
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneId.of("UTC"));
        }
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE email_verifications CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }

    @Test
    void shouldSaveNewVerification() {
        // given
        var user = userFixtures.aUser();
        var tokenHash = "hash-123";
        var expiresAt = NOW.plus(1, ChronoUnit.HOURS);

        // when
        repository.save(user.getId(), tokenHash, expiresAt);

        // then
        var result = emailFixtures.getEmailVerificationByTokenHash(tokenHash);
        assertThat(result).isNotNull();
        assertSoftly(s -> {
            s.assertThat(result.getUserId()).isEqualTo(user.getId());
            s.assertThat(result.getTokenHash()).isEqualTo(tokenHash);
            s.assertThat(result.getExpiresAt()).isEqualTo(expiresAt);
            s.assertThat(result.getCreatedAt()).isEqualTo(NOW);
        });
    }

    @Test
    void shouldUpdateExistingVerificationOnConflict() {
        // given
        var user = userFixtures.aUser();
        emailFixtures.anEmailVerification(
                TestEmailVerification.builder()
                        .userId(user.getId())
                        .tokenHash("old-hash")
                        .expiresAt(NOW.plus(1, ChronoUnit.HOURS))
                        .createdAt(NOW)
                        .build());

        var newTokenHash = "new-hash-456";
        var newExpiresAt = NOW.plus(2, ChronoUnit.HOURS);

        // when
        repository.save(user.getId(), newTokenHash, newExpiresAt);

        // then
        var result = emailFixtures.getEmailVerificationByTokenHash(newTokenHash);

        assertThat(result).isNotNull();
        assertSoftly(s -> {
            s.assertThat(result.getUserId()).isEqualTo(user.getId());
            s.assertThat(result.getTokenHash()).isEqualTo(newTokenHash);
            s.assertThat(result.getExpiresAt()).isEqualTo(newExpiresAt);
            s.assertThat(result.getCreatedAt()).isEqualTo(NOW);
        });

        // old hash is gone
        assertThat(emailFixtures.getEmailVerificationByTokenHash("old-hash")).isNull();
    }

    @Test
    void shouldFindVerificationByTokenHash() {
        // given
        var user = userFixtures.aUser();
        var tokenHash = "unique-hash";
        emailFixtures.anEmailVerification(
                TestEmailVerification.builder()
                        .userId(user.getId())
                        .tokenHash(tokenHash)
                        .expiresAt(NOW.plus(1, ChronoUnit.HOURS))
                        .createdAt(NOW)
                        .build());

        // when
        var result = repository.findByTokenHash(tokenHash);

        // then
        assertThat(result).isPresent();
        assertSoftly(s -> {
            s.assertThat(result.get().userId()).isEqualTo(user.getId());
            s.assertThat(result.get().tokenHash()).isEqualTo(tokenHash);
        });
    }

    @Test
    void shouldReturnEmptyWhenTokenNotFound() {
        // when
        var result = repository.findByTokenHash("non-existent-hash");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldDeleteVerificationByUserId() {
        // given
        var user = userFixtures.aUser();
        var tokenHash = "hash-to-delete";
        emailFixtures.anEmailVerification(
                TestEmailVerification.builder()
                        .userId(user.getId())
                        .tokenHash(tokenHash)
                        .expiresAt(NOW.plus(1, ChronoUnit.HOURS))
                        .createdAt(NOW)
                        .build());

        // when
        repository.deleteByUserId(user.getId());

        // then
        assertThat(emailFixtures.getEmailVerificationByTokenHash(tokenHash)).isNull();
    }

    @Test
    void deleteByUserIdShouldBeIdempotent() {
        // given
        var user = userFixtures.aUser();

        // when
        repository.deleteByUserId(user.getId());

        // then
        var count = emailFixtures.countForUser(user.getId());
        assertThat(count).isZero();
    }
}