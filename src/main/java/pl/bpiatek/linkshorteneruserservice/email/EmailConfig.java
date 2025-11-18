package pl.bpiatek.linkshorteneruserservice.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.bpiatek.linkshorteneruserservice.user.UserFacade;

import java.time.Clock;

@Configuration
class EmailConfig {

    @Bean
    EmailFacade emailFacade(TokenVerificationService tokenVerificationService) {
        return new EmailFacade(tokenVerificationService);
    }

    @Bean
    TokenVerificationService verificationTokenService(EmailVerificationRepository emailVerificationRepository,
                                                      Clock clock,
                                                      @Value("${verification.token.expiration}") long verificationTokenExpirationSec,
                                                      UserFacade userFacade
    ) {
        return new TokenVerificationService(emailVerificationRepository,clock, verificationTokenExpirationSec, userFacade);
    }

    @Bean
    EmailVerificationRepository emailVerificationRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        return new JdbcEmailVerificationRepository(jdbcTemplate, clock);
    }
}
