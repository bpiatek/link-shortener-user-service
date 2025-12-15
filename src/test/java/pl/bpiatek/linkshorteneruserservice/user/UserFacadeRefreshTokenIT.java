package pl.bpiatek.linkshorteneruserservice.user;

import com.google.common.hash.Hashing;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.bpiatek.linkshorteneruserservice.exception.InvalidRefreshTokenException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class UserFacadeRefreshTokenIT extends IntegrationTest {

    @Autowired
    UserFacade userFacade;

    @Autowired
    UserFixtures userFixtures;

    @Autowired
    RefreshTokenFixtures refreshTokenFixtures;

    @Test
    void shouldRotateTokenAndReturnNewPair() {
        // given
        var user = userFixtures.aUser();
        var oldRawToken = "old-raw-token-123";
        refreshTokenFixtures.createActiveToken(user.getId(), oldRawToken);

        // when
        var response = userFacade.refresh(oldRawToken);

        // then
        assertSoftly(s -> {
            s.assertThat(response.accessToken()).isNotBlank();
            s.assertThat(response.refreshToken()).isNotBlank();
            s.assertThat(response.refreshToken()).isNotEqualTo(oldRawToken);
        });

        var tokensInDb = refreshTokenFixtures.findAllByUserId(user.getId());
        assertThat(tokensInDb).hasSize(1);
        assertStoredTokenMatchesRaw(tokensInDb.getFirst(), response.refreshToken());
    }

    @Test
    void shouldThrowExceptionAndRemoveTokenIfExpired() {
        // given
        var user = userFixtures.aUser();
        var expiredRawToken = "expired-token-abc";

        refreshTokenFixtures.createExpiredToken(user.getId(), expiredRawToken);

        // when then
        assertThatThrownBy(() -> userFacade.refresh(expiredRawToken))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token expired");

        var tokensInDb = refreshTokenFixtures.findAllByUserId(user.getId());
        assertThat(tokensInDb).isEmpty();
    }

    @Test
    void shouldThrowExceptionIfTokenDoesNotExist() {
        // given
        var randomToken = "some-random-token-that-does-not-exist";

        // when then
        assertThatThrownBy(() -> userFacade.refresh(randomToken))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token not found in database");
    }

    private void assertStoredTokenMatchesRaw(TestRefreshToken storedToken, String rawToken) {
        var expectedHash = Hashing.sha256()
                .hashString(rawToken, StandardCharsets.UTF_8)
                .toString();

        assertThat(storedToken.getTokenHash()).isEqualTo(expectedHash);
    }
}
