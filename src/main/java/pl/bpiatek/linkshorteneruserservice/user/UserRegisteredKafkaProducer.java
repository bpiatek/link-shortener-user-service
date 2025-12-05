package pl.bpiatek.linkshorteneruserservice.user;

import com.google.protobuf.Timestamp;
import io.micrometer.context.ContextSnapshotFactory;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import pl.bpiatek.contracts.user.UserLifecycleEventProto.UserLifecycleEvent;
import pl.bpiatek.contracts.user.UserLifecycleEventProto.UserRegistered;

import java.time.Clock;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

class UserRegisteredKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredKafkaProducer.class);

    private static final ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder().build();
    private static final String SOURCE_HEADER_VALUE = "user-service";

    private final KafkaTemplate<String, UserLifecycleEvent> kafkaTemplate;
    private final String topicName;
    private final Clock clock;

    UserRegisteredKafkaProducer(KafkaTemplate<String, UserLifecycleEvent> kafkaTemplate,
                                String topicName, Clock clock) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
        this.clock = clock;
    }

    void sendUserRegisteredEvent(String userId, String email, String verificationUrl) {
        log.info("Preparing to send UserRegistered event for userId: {}", userId);
        var eventId = UUID.randomUUID().toString();

        var payload = UserRegistered.newBuilder()
                .setUserId(userId)
                .setEmail(email)
                .setVerificationUrl(verificationUrl)
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

        var producerRecord = new ProducerRecord<>(topicName, userId, event);
        //TODO add later idempotency-key that you get from client calling /reset-password
        producerRecord.headers().add(new RecordHeader("event-id", eventId.getBytes(UTF_8)));
        producerRecord.headers().add(new RecordHeader("source", SOURCE_HEADER_VALUE.getBytes(UTF_8)));

        var snapshot = snapshotFactory.captureAll();

        kafkaTemplate.send(producerRecord).whenComplete((result, ex) -> {
            try (var scope = snapshot.setThreadLocals()) {
                if (ex == null) {
                    log.info("Successfully published UserRegistered event for userId: {} to partition: {} offset: {}",
                            userId,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish UserRegistered event for userId: {}. Reason: {}",
                            userId,
                            ex.getMessage(),
                            ex);
                }
            }
        });
    }
}
