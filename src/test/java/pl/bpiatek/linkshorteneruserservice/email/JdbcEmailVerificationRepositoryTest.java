package pl.bpiatek.linkshorteneruserservice.email;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.bpiatek.linkshorteneruserservice.user.IntegrationTest;
import pl.bpiatek.linkshorteneruserservice.user.UserFixtures;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class JdbcEmailVerificationRepositoryTest extends IntegrationTest {

    @Autowired
    private JdbcEmailVerificationRepository repository;

    @Autowired
    private EmailVerificationFixtures emailFixtures;

    @Autowired
    private UserFixtures userFixtures;

    @Test
    void shouldSaveNewVerification() {
        // given
        var user = userFixtures.aUser();
        var tokenHash = "hash-123";
        var expiresAt = DEFAULT_NOW.plus(1, HOURS);

        // when
        repository.save(user.getId(), tokenHash, expiresAt);

        // then
        var result = emailFixtures.getEmailVerificationByUserId(user.getId());
        assertThat(result).isNotNull();
        assertSoftly(s -> {
            s.assertThat(result.getUserId()).isEqualTo(user.getId());
            s.assertThat(result.getTokenHash()).isEqualTo(tokenHash);
            s.assertThat(result.getExpiresAt()).isEqualTo(expiresAt);
            s.assertThat(result.getCreatedAt()).isEqualTo(DEFAULT_NOW);
        });
    }

    @Test
    void shouldUpdateExistingVerificationOnConflict() {
        // given
        var user = userFixtures.aUser();
        var oldRawToken = "old-raw-token";

        var initialData = TestEmailVerification.builder()
                .userId(user.getId())
                .rawToken(oldRawToken)
                .createdAt(DEFAULT_NOW)
                .expiresAt(DEFAULT_NOW.plus(1, HOURS))
                .build();

        var verification = emailFixtures.anEmailVerification(initialData);

        var newHashToken = "new-hash-token-456";
        var newExpiresAt = DEFAULT_NOW.plus(2, HOURS);

        // when
        repository.save(user.getId(), newHashToken, newExpiresAt);

        // then
        var result = emailFixtures.getEmailVerificationByUserId(user.getId());

        assertThat(result).isNotNull();
        assertSoftly(s -> {
            s.assertThat(result.getUserId()).isEqualTo(user.getId());
            s.assertThat(result.getTokenHash()).isEqualTo(newHashToken);
            s.assertThat(result.getExpiresAt()).isEqualTo(newExpiresAt);
            s.assertThat(result.getCreatedAt()).isEqualTo(DEFAULT_NOW);
            s.assertThat(emailFixtures.getEmailVerificationByTokenHash(verification.getTokenHash())).isNull();
        });
    }

    @Test
    void shouldFindVerificationByTokenHash() {
        // given
        var user = userFixtures.aUser();
        var verification = emailFixtures.anEmailVerification(
                TestEmailVerification.builder()
                        .userId(user.getId())
                        .tokenHash("raw-token")
                        .expiresAt(DEFAULT_NOW.plus(1, HOURS))
                        .createdAt(DEFAULT_NOW)
                        .build());

        // when
        var result = repository.findByTokenHash(verification.getTokenHash());

        // then
        assertThat(result).isPresent();
        assertSoftly(s -> {
            s.assertThat(result.get().userId()).isEqualTo(user.getId());
            s.assertThat(result.get().tokenHash()).isEqualTo(verification.getTokenHash());
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
                        .expiresAt(DEFAULT_NOW.plus(1, HOURS))
                        .createdAt(DEFAULT_NOW)
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