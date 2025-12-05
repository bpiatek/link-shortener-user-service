package pl.bpiatek.linkshorteneruserservice.password;

import com.google.protobuf.Timestamp;
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

class PasswordChangedKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(PasswordChangedKafkaProducer.class);

    private static final String SOURCE_HEADER_VALUE = "user-service";

    private final KafkaTemplate<String, UserLifecycleEvent> kafkaTemplate;
    private final String topicName;
    private final Clock clock;

    PasswordChangedKafkaProducer(KafkaTemplate<String, UserLifecycleEvent> kafkaTemplate, String topicName, Clock clock) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
        this.clock = clock;
    }

    void sendPasswordChangedEvent(String userId, String email) {
        log.info("Preparing to send PasswordChanged event for userId: {}", userId);
        var eventId = UUID.randomUUID().toString();

        var payload = UserLifecycleEventProto.UserPasswordChanged.newBuilder()
                .setUserId(userId)
                .setEmail(email)
                .build();

        var now = clock.instant();
        var event = UserLifecycleEvent.newBuilder()
                .setEventId(eventId)
                .setEventTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .setUserPasswordChanged(payload)
                .build();

        var producerRecord = new ProducerRecord<>(topicName, userId, event);
        //TODO add later idempotency-key that you get from client calling /reset-password
        producerRecord.headers().add(new RecordHeader("event-id", eventId.getBytes(UTF_8)));
        producerRecord.headers().add(new RecordHeader("source", SOURCE_HEADER_VALUE.getBytes(UTF_8)));

        kafkaTemplate.send(producerRecord).whenComplete((result, ex) -> {
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
        });
    }
}
