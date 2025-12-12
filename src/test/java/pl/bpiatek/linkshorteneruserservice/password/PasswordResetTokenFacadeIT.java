package pl.bpiatek.linkshorteneruserservice.password;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.bpiatek.linkshorteneruserservice.exception.InvalidTokenException;
import pl.bpiatek.linkshorteneruserservice.user.IntegrationTest;
import pl.bpiatek.linkshorteneruserservice.user.UserFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static pl.bpiatek.linkshorteneruserservice.password.TestPasswordResetToken.builder;

class PasswordResetTokenFacadeIT extends IntegrationTest {

    @Autowired
    PasswordResetTokenFacade facade;

    @Autowired
    UserFixtures userFixtures;

    @Autowired
    PasswordResetTokenFixtures tokenFixtures;

    @Test
    void shouldDoNotThrowWhenRequestingPasswordResetForUnknownUser() {
        // given
        var nonExistentUser = "test@test.com";

        // when then
        assertThatCode(() -> facade.requestPasswordReset(nonExistentUser))
                .doesNotThrowAnyException();
        assertThat(events.stream(PasswordResetApplicationEvent.class)).isEmpty();
    }

    @Test
    void shouldRequestPasswordResetForUser() {
        // given
        var user = userFixtures.aUser();
        var email = user.getEmail();

        // when
        facade.requestPasswordReset(email);

        // then
        var token = tokenFixtures.getTokenByUserId(user.getId());
        assertThat(token).isNotNull();
        assertSoftly(s -> {
           s.assertThat(token.getUserId()).isEqualTo(user.getId());
           s.assertThat(token.getTokenHash()).isNotEmpty();
           s.assertThat(token.getCreatedAt()).isEqualTo(DEFAULT_NOW);
           s.assertThat(token.getExpiresAt()).isEqualTo(DEFAULT_NOW.plusSeconds(3600));

            s.assertThat(events.stream(PasswordResetApplicationEvent.class))
                    .hasSize(1)
                    .first()
                    .satisfies(event -> {
                        assertThat(event.email()).isEqualTo(email);
                        assertThat(event.userId()).isEqualTo(user.getId().toString());
                        assertThat(event.resetUrl()).contains("reset-password");
                    });
        });
    }

    @Test
    void shouldThrowOnInvalidResetPasswordToken() {
        // given
        var token = "dummy-token";

        // when then
        assertThatCode(() -> facade.resetPassword(token, "new-password"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining(token);
    }

    @Test
    void shouldUpdatePassword() {
        // given
        var newPassword = "pass";
        var user = userFixtures.aUser();
        var token = tokenFixtures.aPasswordResetToken(builder().userId(user.getId()).build());
        setCurrentTime(token.getCreatedAt());

        // when
        facade.resetPassword(token.getToken(), newPassword);

        // then
        assertSoftly(s -> {
            var consumedToken = tokenFixtures.getTokenByUserId(user.getId());
            s.assertThat(consumedToken).isNull();

            s.assertThat(events.stream(PasswordChangedApplicationEvent.class))
                    .hasSize(1)
                    .first()
                    .satisfies(event -> {
                        assertThat(event.email()).isEqualTo(user.getEmail());
                        assertThat(event.userId()).isEqualTo(user.getId().toString());
                    });
        });

    }
}