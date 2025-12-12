package pl.bpiatek.linkshorteneruserservice.password;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.bpiatek.linkshorteneruserservice.user.IntegrationTest;
import pl.bpiatek.linkshorteneruserservice.user.UserFixtures;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class JdbcPasswordResetTokenRepositoryTest extends IntegrationTest {

    @Autowired
    JdbcPasswordResetTokenRepository repository;

    @Autowired
    PasswordResetTokenFixtures tokenFixtures;

    @Autowired
    UserFixtures userFixtures;

    @Test
    void shouldSaveNewToken() {
        // given
        var user = userFixtures.aUser();
        var tokenHash = "hash-123";
        var expiresAt = DEFAULT_NOW.plus(15, MINUTES);

        // when
        repository.save(user.getId(), tokenHash, expiresAt);

        // then
        var foundToken = tokenFixtures.getTokenByUserId(user.getId());
        assertThat(foundToken).isNotNull();
        assertSoftly(softly -> {
            softly.assertThat(foundToken.getUserId()).isEqualTo(user.getId());
            softly.assertThat(foundToken.getTokenHash()).isEqualTo(tokenHash);
            softly.assertThat(foundToken.getExpiresAt()).isEqualTo(expiresAt);
            softly.assertThat(foundToken.getCreatedAt()).isEqualTo(DEFAULT_NOW);
        });
    }

    @Test
    void shouldUpdateExistingTokenOnConflict() {
        // given
        var user = userFixtures.aUser();
        tokenFixtures.aPasswordResetToken(
                TestPasswordResetToken.builder()
                        .userId(user.getId())
                        .tokenHash("old-hash")
                        .expiresAt(DEFAULT_NOW.plus(10, MINUTES))
                        .build()
        );

        var newTokenHash = "new-hash-456";
        var newExpiresAt = DEFAULT_NOW.plus(30, MINUTES);

        // when
        repository.save(user.getId(), newTokenHash, newExpiresAt);

        // then
        var foundToken = tokenFixtures.getTokenByUserId(user.getId());
        assertThat(foundToken).isNotNull();
        assertSoftly(softly -> {
            softly.assertThat(foundToken.getTokenHash()).isEqualTo(newTokenHash);
            softly.assertThat(foundToken.getExpiresAt()).isEqualTo(newExpiresAt);
            softly.assertThat(foundToken.getCreatedAt()).isEqualTo(DEFAULT_NOW);
        });

        var count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM password_reset_tokens", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldFindTokenByHash() {
        // given
        var user = userFixtures.aUser();
        var token = "unique-abc";

        var savedResetToken = tokenFixtures.aPasswordResetToken(TestPasswordResetToken.builder()
                .userId(user.getId())
                .token(token)
                .expiresAt(DEFAULT_NOW.plus(1, HOURS))
                .createdAt(DEFAULT_NOW)
                .build());

        // when
        var result = repository.findByTokenHash(savedResetToken.getTokenHash());

        // then
        assertThat(result).isPresent();
        assertSoftly(softly -> {
            var resetToken = result.get();
            softly.assertThat(resetToken.userId()).isEqualTo(user.getId());
            softly.assertThat(resetToken.tokenHash()).isEqualTo(savedResetToken.getTokenHash());
            softly.assertThat(resetToken.expiresAt()).isEqualTo(DEFAULT_NOW.plus(1, HOURS));
            softly.assertThat(resetToken.createdAt()).isEqualTo(DEFAULT_NOW);
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
    void shouldDeleteTokenByUserId() {
        // given
        var user = userFixtures.aUser();
        repository.save(user.getId(), "hash-to-delete", DEFAULT_NOW.plus(1, HOURS));

        // when
        repository.deleteByUserId(user.getId());

        // then
        var foundToken = tokenFixtures.getTokenByUserId(user.getId());
        assertThat(foundToken).isNull();
    }
}