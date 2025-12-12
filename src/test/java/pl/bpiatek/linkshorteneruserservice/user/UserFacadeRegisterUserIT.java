package pl.bpiatek.linkshorteneruserservice.user;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import pl.bpiatek.contracts.user.UserLifecycleEventProto.UserLifecycleEvent;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class UserFacadeRegisterUserIT extends IntegrationTest {

    @Autowired
    UserFacade userFacade;

    @Autowired
    UserFixtures userFixtures;

    @Value("${app.verification-url}")
    String appVerificationUrl;

    @Test
    void shouldRegisterUser() {
        // given
        var email = "test@example.com";
        var password = "password";

        // when
        userFacade.register(email, password);

        // then
        var user = userFixtures.getUserByEmail(email);
        assertThat(user).isNotNull();
        assertSoftly(s -> {
            s.assertThat(user.getEmail()).isEqualTo(email);
            s.assertThat(user.getPasswordHash()).isNotEqualTo(password);
        });
    }

    @Test
    void shouldSendUserRegisteredEventWhenUserIsRegistered() throws InterruptedException {
        // given
        var email = "test@example.com";
        var password = "password";

        // when
        userFacade.register(email, password);

        // then
        var record = testUserLifecycleEventConsumer.awaitRecord(5, TimeUnit.SECONDS);
        assertThat(record).isNotNull();

        var user = userFixtures.getUserByEmail(email);
        assertThat(user).isNotNull();

        assertSoftly(s -> {
            var envelope = record.value();
            s.assertThat(envelope.getEventPayloadCase()).isEqualTo(UserLifecycleEvent.EventPayloadCase.USER_REGISTERED);

            var message = envelope.getUserRegistered();
            s.assertThat(message.getUserId()).isEqualTo(user.getId().toString());
            s.assertThat(message.getEmail()).isEqualTo(email);
            s.assertThat(message.getVerificationUrl()).contains(appVerificationUrl);
        });
    }

    @Test
    void shouldLoginUser() throws NoSuchAlgorithmException {
        // given
        var mail = "test@example.com";
        var password = "password";
        userFacade.register(mail, password);

        // when
        var response = userFacade.login(mail, password);

        // then
        assertSoftly(s -> {
            s.assertThat(response.accessToken()).isNotEmpty();
            s.assertThat(response.refreshToken()).isNotEmpty();
        });
    }

    @Test
    void shouldNotLoginUserWhenItWasNotRegistered() {
        // when then
        assertThatThrownBy(() -> userFacade.login("test@example.com", "password"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Bad credentials");
    }
}