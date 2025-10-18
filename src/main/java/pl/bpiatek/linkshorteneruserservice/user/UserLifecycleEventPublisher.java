package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

class UserLifecycleEventPublisher {

    private final UserKafkaProducer userKafkaProducer;

    UserLifecycleEventPublisher(UserKafkaProducer userKafkaProducer) {
        this.userKafkaProducer = userKafkaProducer;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleUserRegisteredEvent(UserRegisteredApplicationEvent event) {
        userKafkaProducer.sendUserRegisteredEvent(
                event.userId(),
                event.email(),
                event.verificationToken()
        );
    }
}
