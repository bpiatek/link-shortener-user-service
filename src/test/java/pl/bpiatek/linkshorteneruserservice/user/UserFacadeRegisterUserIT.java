package pl.bpiatek.linkshorteneruserservice.user;


import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pl.bpiatek.contracts.user.UserLifecycleEventProto.UserLifecycleEvent;
import pl.bpiatek.linkshorteneruserservice.WithFullInfrastructure;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@SpringBootTest
@ActiveProfiles("test")
class UserFacadeRegisterUserIT implements WithFullInfrastructure {

    @Autowired
    UserFacade userFacade;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    UserFixtures userFixtures;

    @Autowired
    TestKafkaConsumer<UserLifecycleEvent> testConsumer;

    @MockitoBean
    LoginService loginService;

    @MockitoBean
    JwtKeyProvider jwtKeyProvider;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        var eventType = "pl.bpiatek.contracts.user.UserLifecycleEventProto$UserLifecycleEvent";
        registry.add("spring.kafka.producer.properties.specific.protobuf.value.type",
                () -> eventType);
        registry.add("spring.kafka.consumer.properties.specific.protobuf.value.type",
                () -> eventType);
        registry.add("spring.kafka.bootstrap-servers", redpanda::getBootstrapServers);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM email_verifications");
        jdbcTemplate.update("DELETE FROM users");
        testConsumer.reset();
    }

    @Test
    void shouldRegisterUser() throws InterruptedException {
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
        var record = testConsumer.awaitRecord(5, TimeUnit.SECONDS);
        assertThat(record).isNotNull();

        var user = userFixtures.getUserByEmail(email);
        assertThat(user).isNotNull();

        assertSoftly(s -> {
            var envelope = record.value();
            s.assertThat(envelope.getEventPayloadCase()).isEqualTo(UserLifecycleEvent.EventPayloadCase.USER_REGISTERED);

            var message = envelope.getUserRegistered();
            s.assertThat(message.getUserId()).isEqualTo(user.getId().toString());
            s.assertThat(message.getEmail()).isEqualTo(email);
            s.assertThat(message.getVerificationToken()).isNotNull();
        });
    }

    @TestConfiguration
    static class KafkaTestConsumerConfiguration {

        @Bean
        public TestKafkaConsumer<UserLifecycleEvent> testUserRegisteredEventConsumer() {
            return new TestKafkaConsumer<>();
        }

        @KafkaListener(
                topics = "${topic.user.lifecycle}",
                groupId = "test-user-registered-event-consumer-group"
        )
        public void listen(ConsumerRecord<String, UserLifecycleEvent> record) {
            testUserRegisteredEventConsumer().handle(record);
        }
    }
}