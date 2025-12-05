package pl.bpiatek.linkshorteneruserservice.password;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

class PasswordEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PasswordEventPublisher.class);

    private final PasswordResetKafkaProducer passwordResetKafkaProducer;
    private final PasswordChangedKafkaProducer passwordChangedKafkaProducer;

    PasswordEventPublisher(PasswordResetKafkaProducer passwordResetKafkaProducer,
                           PasswordChangedKafkaProducer passwordChangedKafkaProducer) {
        this.passwordResetKafkaProducer = passwordResetKafkaProducer;
        this.passwordChangedKafkaProducer = passwordChangedKafkaProducer;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handlePasswordResetEvent(PasswordResetApplicationEvent event) {
        log.info("Publishing PasswordReset event for user ID: {}", event.userId());
        passwordResetKafkaProducer.sendPasswordResetRequestedEvent(
                event.userId(),
                event.email(),
                event.resetUrl()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handlePasswordUpdatedEvent(PasswordChangedApplicationEvent event) {
        log.info("Publishing PasswordUpdated event for user ID: {}", event.userId());
        passwordChangedKafkaProducer.sendPasswordChangedEvent(
                event.userId(),
                event.email()
        );
    }
}
