package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

class UserLifecycleEventPublisher {

    private final UserRegisteredKafkaProducer userRegisteredKafkaProducer;

    UserLifecycleEventPublisher(UserRegisteredKafkaProducer userRegisteredKafkaProducer) {
        this.userRegisteredKafkaProducer = userRegisteredKafkaProducer;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleUserRegisteredEvent(UserRegisteredApplicationEvent event) {
        userRegisteredKafkaProducer.sendUserRegisteredEvent(
                event.userId(),
                event.email(),
                event.verificationToken()
        );
    }
}
