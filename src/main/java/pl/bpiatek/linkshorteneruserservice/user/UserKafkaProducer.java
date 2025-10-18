package pl.bpiatek.linkshorteneruserservice.user;

import com.google.protobuf.Timestamp;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import pl.bpiatek.contracts.user.UserLifecycleEventProto;
import pl.bpiatek.contracts.user.UserLifecycleEventProto.UserLifecycleEvent;
import pl.bpiatek.contracts.user.UserLifecycleEventProto.UserRegistered;

import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static java.nio.charset.StandardCharsets.UTF_8;

class UserKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(UserKafkaProducer.class);
    private static final String SOURCE_HEADER_VALUE = "user-service";

    private final KafkaTemplate<String, UserLifecycleEvent> kafkaTemplate;
    private final String topicName;
    private final Clock clock;

    UserKafkaProducer(KafkaTemplate<String, UserLifecycleEvent> kafkaTemplate,
                      String topicName, Clock clock) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
        this.clock = clock;
    }

    void sendUserRegisteredEvent(String userId, String email, String verificationToken) {
        log.info("Preparing to send UserRegistered event for userId: {}", userId);
        var signature = userId + email;
        var eventId = UUID.nameUUIDFromBytes(signature.getBytes(UTF_8)).toString();

        var payload = UserRegistered.newBuilder()
                .setUserId(userId)
                .setEmail(email)
                .setVerificationToken(verificationToken)
                .build();

        var now = clock.instant();
        var event = UserLifecycleEvent.newBuilder()
                .setEventId(eventId)
                .setEventTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .setUserRegistered(payload)
                .build();

        var producerRecord = new ProducerRecord<>(topicName, eventId, event);
        producerRecord.headers().add(new RecordHeader("trace-id", UUID.randomUUID().toString().getBytes(UTF_8)));
        producerRecord.headers().add(new RecordHeader("source", SOURCE_HEADER_VALUE.getBytes(UTF_8)));

        try {
            var result = kafkaTemplate.send(producerRecord).get();
            log.info("Successfully published UserRegistered event for userId: {} to partition: {} offset: {}",
                    userId,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to publish UserRegistered event for userId: {}. Reason: {}",
                    userId,
                    e.getMessage());

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    void sendPasswordResetRequestedEvent(/*...params...*/) {
        log.warn("sendPasswordResetRequestedEvent is not yet implemented.");
    }

    void sendUserPasswordChangedEvent(/*...params...*/) {
        log.warn("sendUserPasswordChangedEvent is not yet implemented.");
    }

}
