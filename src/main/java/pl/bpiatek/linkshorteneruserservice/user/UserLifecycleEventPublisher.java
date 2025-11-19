package pl.bpiatek.linkshorteneruserservice.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.bpiatek.linkshorteneruserservice.email.EmailFacade;

class UserLifecycleEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserLifecycleEventPublisher.class);

    private final UserRegisteredKafkaProducer userRegisteredKafkaProducer;
    private final EmailFacade emailFacade;
    private final String appBaseUrl;
    private final String appVerificationUrl;

    UserLifecycleEventPublisher(UserRegisteredKafkaProducer userRegisteredKafkaProducer, EmailFacade emailFacade, String appBaseUrl, String appVerificationUrl) {
        this.userRegisteredKafkaProducer = userRegisteredKafkaProducer;
        this.emailFacade = emailFacade;
        this.appBaseUrl = appBaseUrl;
        this.appVerificationUrl = appVerificationUrl;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleUserRegisteredEvent(UserRegisteredApplicationEvent event) {
        var rawToken = emailFacade.generateAndSaveToken(
                Long.valueOf(event.userId()),
                event.email()
        );

        var verificationUrl = appBaseUrl + appVerificationUrl + rawToken;

        log.info("Saved verification token for user ID: {}", event.userId());

        userRegisteredKafkaProducer.sendUserRegisteredEvent(
                event.userId(),
                event.email(),
                verificationUrl
        );
    }
}
