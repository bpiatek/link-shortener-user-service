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

    UserLifecycleEventPublisher(UserRegisteredKafkaProducer userRegisteredKafkaProducer, EmailFacade emailFacade) {
        this.userRegisteredKafkaProducer = userRegisteredKafkaProducer;
        this.emailFacade = emailFacade;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleUserRegisteredEvent(UserRegisteredApplicationEvent event) {
        var rawToken = emailFacade.generateAndSaveToken(
                Long.valueOf(event.userId()),
                event.email()
        );

        log.info("Saved verification token for user ID: {}", event.userId());

        userRegisteredKafkaProducer.sendUserRegisteredEvent(
                event.userId(),
                event.email(),
                rawToken
        );
    }
}
