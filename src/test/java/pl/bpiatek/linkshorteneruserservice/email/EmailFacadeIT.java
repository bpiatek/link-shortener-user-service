package pl.bpiatek.linkshorteneruserservice.email;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.bpiatek.linkshorteneruserservice.exception.ExpiredTokenException;
import pl.bpiatek.linkshorteneruserservice.exception.InvalidTokenException;
import pl.bpiatek.linkshorteneruserservice.user.IntegrationTest;
import pl.bpiatek.linkshorteneruserservice.user.UserFixtures;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class EmailFacadeIT extends IntegrationTest {

    @Autowired
    private EmailFacade facade;

    @Autowired
    private EmailVerificationFixtures emailFixtures;

    @Autowired
    private UserFixtures userFixtures;

    @Test
    void shouldGenerateAndSaveVerificationToken() {
        // given
        var user = userFixtures.aUser();

        // when
        facade.generateAndSaveEmailVerificationToken(user.getId().toString(), user.getEmail());

        // then
        var verification = emailFixtures.getEmailVerificationByUserId(user.getId());
        assertThat(verification).isNotNull();
        assertSoftly(s -> {
            s.assertThat(verification.getUserId()).isEqualTo(user.getId());
            s.assertThat(verification.getTokenHash()).isNotEmpty();
        });
    }

    @Test
    void shouldVerifyEmailByRawToken() {
        // given
        var user = userFixtures.aUser();
        var rawToken = "test-token";
        emailFixtures.anEmailVerification(TestEmailVerification.builder()
                .userId(user.getId())
                .rawToken(rawToken)
                .expiresAt(DEFAULT_NOW.plus(10, MINUTES))
                .build());

        // when
        facade.verifyEmail(rawToken);

        // then
        var verifiedUser = userFixtures.getUserByEmail(user.getEmail());
        assertThat(verifiedUser).isNotNull();
        assertSoftly(s -> {
            s.assertThat(verifiedUser.getId()).isEqualTo(user.getId());
            s.assertThat(verifiedUser.isEmailVerified()).isTrue();
        });
    }

    @Test
    void shouldThrowWhenVerificationTokenExpired() {
        // given
        var rawToken = "test-token";
        var user = userFixtures.aUser();
        emailFixtures.anEmailVerification(TestEmailVerification.builder()
                .userId(user.getId())
                .rawToken(rawToken)
                .expiresAt(DEFAULT_NOW.minus(10, MINUTES))
                .build());

        // when then
        assertThatCode(() -> facade.verifyEmail(rawToken))
                .isInstanceOf(ExpiredTokenException.class)
                .hasMessage("Verification token has expired.");
    }

    @Test
    void shouldThrowWhenInvalidTokenProvided() {
        // given
        var rawToken = "non-existing-token";
        userFixtures.aUser();

        // when then
        assertThatCode(() -> facade.verifyEmail(rawToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid verification token.");
    }
}