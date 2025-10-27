package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.bpiatek.contracts.user.UserLifecycleEventProto.UserLifecycleEvent;

import java.time.Clock;

@Configuration
class UserConfig {

    @Bean
    RoleCache roleCache(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new RoleCache(namedParameterJdbcTemplate);
    }

    @Bean
    UserRepository userRepository(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            RoleCache roleCache) {
        return new JdbcUserRepository(namedParameterJdbcTemplate, roleCache);
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository userRepository) {
        return new UserDetailsServiceImpl(userRepository);
    }

    @Bean
    UserFacade userFacade(RegistrationService registrationService, LoginService loginService, JwtKeyProvider jwtKeyProvider) {
        return new UserFacade(registrationService, loginService, jwtKeyProvider);
    }

    @Bean
    LoginService loginService(AuthenticationManager authenticationManager, JwtService jwtService) {
        return new LoginService(authenticationManager, jwtService);
    }

    @Bean
    RegistrationService registrationService(UserRepository userRepository,
                                            PasswordEncoder passwordEncoder,
                                            Clock clock,
                                            ApplicationEventPublisher eventPublisher,
                                            VerificationTokenService verificationTokenService) {
        return new RegistrationService(userRepository, passwordEncoder, clock, eventPublisher, verificationTokenService);
    }

    @Bean
    VerificationTokenService verificationTokenService(EmailVerificationRepository emailVerificationRepository,
                                                      PasswordEncoder passwordEncoder,
                                                      Clock clock,
                                                      @Value("${verification.token.expiration}") long verificationTokenExpirationSec
    ) {
     return new VerificationTokenService(emailVerificationRepository, passwordEncoder, clock, verificationTokenExpirationSec);
    }

    @Bean
    JwtService jwtService(@Value("${app.jwt.access-token.expiration}") long accessTokenExpiration,
                          @Value("${app.jwt.refresh-token.expiration}") long refreshTokenExpiration,
                          Clock clock,
                          UserRepository userRepository,
                          JwtKeyProvider jwtKeyProvider) {
        return new JwtService(accessTokenExpiration, refreshTokenExpiration, clock, userRepository, jwtKeyProvider);
    }

    @Bean
    JwtKeyProvider jwtKeyProvider(@Value("${vault.secrets.path:/vault/secrets/jwtkeyjson}") String keyFilePath) {
        return new JwtKeyProvider(keyFilePath);
    }

    @Bean
    UserRegisteredKafkaProducer userKafkaProducer(KafkaTemplate<String, UserLifecycleEvent> kafkaTemplate,
                                                  @Value("${topic.user.lifecycle}") String topicName,
                                                  Clock clock) {
       return new UserRegisteredKafkaProducer(kafkaTemplate, topicName, clock);
    }

    @Bean
    UserLifecycleEventPublisher userLifecycleEventPublisher(UserRegisteredKafkaProducer userRegisteredKafkaProducer) {
        return new UserLifecycleEventPublisher(userRegisteredKafkaProducer);
    }

    @Bean
    EmailVerificationRepository emailVerificationRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        return new JdbcEmailVerificationRepository(jdbcTemplate, clock);
    }
}
