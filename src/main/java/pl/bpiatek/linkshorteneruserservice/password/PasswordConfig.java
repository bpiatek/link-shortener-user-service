package pl.bpiatek.linkshorteneruserservice.password;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.bpiatek.contracts.user.UserLifecycleEventProto;
import pl.bpiatek.linkshorteneruserservice.user.UserFacade;

import java.time.Clock;

@Configuration
class PasswordConfig {

    @Bean
    PasswordResetTokenRepository passwordResetTokenRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        return new JdbcPasswordResetTokenRepository(jdbcTemplate, clock);
    }

    @Bean
    PasswordEventPublisher passwordResetEventPublisher(PasswordResetKafkaProducer passwordResetKafkaProducer,
                                                       PasswordChangedKafkaProducer passwordChangedKafkaProducer) {
        return new PasswordEventPublisher(passwordResetKafkaProducer, passwordChangedKafkaProducer);
    }

    @Bean
    PasswordChangedKafkaProducer passwordChangedKafkaProducer(KafkaTemplate<String, UserLifecycleEventProto.UserLifecycleEvent> kafkaTemplate,
                                                              @Value("${topic.user.lifecycle}") String topicName,
                                                              Clock clock) {
        return new PasswordChangedKafkaProducer(kafkaTemplate, topicName, clock);
    }

    @Bean
    PasswordResetKafkaProducer passwordResetKafkaProducer(KafkaTemplate<String, UserLifecycleEventProto.UserLifecycleEvent> kafkaTemplate,
                                                          @Value("${topic.user.lifecycle}") String topicName,
                                                          Clock clock) {
        return new PasswordResetKafkaProducer(kafkaTemplate, topicName, clock);
    }

    @Bean
    PasswordResetTokenService passwordResetTokenService(PasswordResetTokenRepository repository,
                                                        Clock clock,
                                                        @Value("${password.reset.token.expiration}") long expirationSec) {
        return new PasswordResetTokenService(repository, clock, expirationSec);
    }

    @Bean
    PasswordResetTokenFacade passwordResetTokenFacade(UserFacade userFacade,
                                                      PasswordResetTokenService passwordResetTokenService,
                                                      ApplicationEventPublisher applicationEventPublisher,
                                                      @Value("${app.base-url}") String appBaseUrl,
                                                      @Value("${app.reset-password-url}") String resetPasswordUrl,
                                                      PasswordEncoder passwordEncoder) {
        return new PasswordResetTokenFacade(
                userFacade,
                passwordResetTokenService,
                applicationEventPublisher,
                appBaseUrl,
                resetPasswordUrl,
                passwordEncoder);
    }
}
