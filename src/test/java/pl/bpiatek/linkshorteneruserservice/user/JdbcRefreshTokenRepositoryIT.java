package pl.bpiatek.linkshorteneruserservice.user;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class JdbcRefreshTokenRepositoryIT extends IntegrationTest {

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    UserFixtures userFixtures;

    @Autowired
    RefreshTokenFixtures refreshTokenFixtures;

    @Test
    void shouldSaveRefreshToken() {
        // given
        var user = userFixtures.aUser();

        var refreshToken = new RefreshToken(
                null,
                user.getId(),
                "hashed_refresh_token_123",
                DEFAULT_NOW.plus(7, DAYS),
                DEFAULT_NOW);

        // when
        refreshTokenRepository.save(refreshToken);

        // then
        var foundRefreshToken = refreshTokenFixtures.getRefreshTokenForUser(user.getId());
        assertThat(foundRefreshToken).isNotNull();
        assertSoftly(s -> {
           s.assertThat(foundRefreshToken.getId()).isNotNull();
           s.assertThat(foundRefreshToken.getUserId()).isEqualTo(user.getId());
           s.assertThat(foundRefreshToken.getTokenHash()).isEqualTo(refreshToken.tokenHash());
           s.assertThat(foundRefreshToken.getExpiresAt()).isEqualTo(refreshToken.expiresAt());
           s.assertThat(foundRefreshToken.getCreatedAt()).isEqualTo(refreshToken.createdAt());
        });
    }

    @Test
    void shouldFindByTokenHash() {
        // given
        var user = userFixtures.aUser();
        var refreshToken = refreshTokenFixtures.aRefreshToken(user.getId());

        // when
        var optionalRefreshToken = refreshTokenRepository.findByTokenHash(refreshToken.getTokenHash());

        // then
        assertThat(optionalRefreshToken).isPresent();
        var foundRefreshToken = optionalRefreshToken.get();
        assertSoftly(s -> {
          s.assertThat(refreshToken.getId()).isEqualTo(foundRefreshToken.id());
          s.assertThat(refreshToken.getUserId()).isEqualTo(foundRefreshToken.userId());
          s.assertThat(refreshToken.getTokenHash()).isEqualTo(foundRefreshToken.tokenHash());
          s.assertThat(refreshToken.getExpiresAt()).isEqualTo(foundRefreshToken.expiresAt());
          s.assertThat(refreshToken.getCreatedAt()).isEqualTo(foundRefreshToken.createdAt());
        });
    }

    @Test
    void shouldReturnEmptyWhenTokenNotFoundByTokenHash() {
        // given no entry in database

        // when
        var optionalRefreshToken = refreshTokenRepository.findByTokenHash("non-existent-hash");

        // then
        assertThat(optionalRefreshToken).isEmpty();
    }

    @Test
    void shouldDeleteByTokenId() {
        // given
        var user = userFixtures.aUser();
        var refreshToken = refreshTokenFixtures.aRefreshToken(user.getId());

        // when
        refreshTokenRepository.deleteById(refreshToken.getId());

        // then
        var foundRefreshToken = refreshTokenFixtures.getRefreshTokenById(refreshToken.getId());
        assertThat(foundRefreshToken).isNull();
    }
}