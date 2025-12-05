package pl.bpiatek.linkshorteneruserservice.password;

import com.google.protobuf.Timestamp;
import io.micrometer.context.ContextSnapshotFactory;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import pl.bpiatek.contracts.user.UserLifecycleEventProto;
import pl.bpiatek.contracts.user.UserLifecycleEventProto.UserLifecycleEvent;

import java.time.Clock;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

class PasswordResetKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetKafkaProducer.class);

    private static final ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder().build();
    private static final String SOURCE_HEADER_VALUE = "user-service";

    private final KafkaTemplate<String, UserLifecycleEvent> kafkaTemplate;
    private final String topicName;
    private final Clock clock;

    PasswordResetKafkaProducer(KafkaTemplate<String, UserLifecycleEvent> kafkaTemplate, String topicName, Clock clock) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
        this.clock = clock;
    }

    void sendPasswordResetRequestedEvent(String userId, String email, String resetUrl) {
        log.info("Preparing to send PasswordReset event for userId: {}", userId);

        var eventId = UUID.randomUUID().toString();

        var payload = UserLifecycleEventProto.PasswordResetRequested.newBuilder()
                .setUserId(userId)
                .setEmail(email)
                .setResetUrl(resetUrl)
                .build();

        var now = clock.instant();
        var event = UserLifecycleEvent.newBuilder()
                .setEventId(eventId)
                .setEventTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .setPasswordResetRequested(payload)
                .build();

        var producerRecord = new ProducerRecord<>(topicName, userId, event);
        //TODO add later idempotency-key that you get from client calling /reset-password
        producerRecord.headers().add(new RecordHeader("event-id", eventId.getBytes(UTF_8)));
        producerRecord.headers().add(new RecordHeader("source", SOURCE_HEADER_VALUE.getBytes(UTF_8)));

        var snapshot = snapshotFactory.captureAll();

        kafkaTemplate.send(producerRecord).whenComplete((result, ex) -> {
            try (var scope = snapshot.setThreadLocals()) {
                if (ex == null) {
                    log.info("Successfully published PasswordChanged event for userId: {} to partition: {} offset: {}",
                            userId,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish PasswordChanged event for userId: {}. Reason: {}",
                            userId,
                            ex.getMessage(),
                            ex);
                }
            }
        });
    }
}
