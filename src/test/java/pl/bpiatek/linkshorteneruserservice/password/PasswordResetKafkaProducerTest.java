package pl.bpiatek.linkshorteneruserservice.password;


import nl.altindag.log.LogCaptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import pl.bpiatek.contracts.user.UserLifecycleEventProto.UserLifecycleEvent;
import pl.bpiatek.linkshorteneruserservice.exception.KafkaEventSendingException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PasswordResetKafkaProducerTest {

    private static final String TEST_TOPIC = "test-topic";
    private static final String USER_ID = "1";
    private static final String EMAIL = "test@example.com";
    private static final String RESET_URL = "https://app.com/reset";

    @Mock
    private KafkaTemplate<String, UserLifecycleEvent> kafkaTemplate;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, UserLifecycleEvent>> producerRecordCaptor;

    private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneOffset.UTC);
    private LogCaptor logCaptor;

    private PasswordResetKafkaProducer producer;

    @BeforeEach
    void setUp() {
        producer = new PasswordResetKafkaProducer(kafkaTemplate, TEST_TOPIC, clock);
        logCaptor = LogCaptor.forClass(PasswordResetKafkaProducer.class);
    }

    @AfterEach
    void tearDown() {
        logCaptor.clearLogs();
    }

    @Test
    void shouldSendMessageWithCorrectData() {
        // given
        mockSuccessfulSend();

        // when
        producer.sendPasswordResetRequestedEvent(USER_ID, EMAIL, RESET_URL);

        // then
        verify(kafkaTemplate).send(producerRecordCaptor.capture());
        var record = producerRecordCaptor.getValue();

        assertSoftly(softly -> {
            // Basics
            softly.assertThat(record.topic()).isEqualTo(TEST_TOPIC);
            softly.assertThat(record.key()).isEqualTo(USER_ID);

            // Headers
            var sourceHeader = record.headers().lastHeader("source");
            softly.assertThat(sourceHeader).isNotNull();
            softly.assertThat(new String(sourceHeader.value(), UTF_8)).isEqualTo("user-service");
            softly.assertThat(record.headers().lastHeader("event-id")).isNotNull();

            // Payload
            var event = record.value();
            softly.assertThat(event.getEventId()).isNotBlank(); // Better than isNotNull
            softly.assertThat(event.getEventTimestamp().getSeconds()).isEqualTo(clock.instant().getEpochSecond());

            var payload = event.getPasswordResetRequested();
            softly.assertThat(payload.getUserId()).isEqualTo(USER_ID);
            softly.assertThat(payload.getEmail()).isEqualTo(EMAIL);
            softly.assertThat(payload.getResetUrl()).isEqualTo(RESET_URL);
        });
    }

    @Test
    void shouldLogInfoOnAsyncSuccess() {
        // given
        mockSuccessfulSend();

        // when
        producer.sendPasswordResetRequestedEvent(USER_ID, EMAIL, RESET_URL);

        // then
        assertThat(logCaptor.getInfoLogs())
                .anySatisfy(log -> assertThat(log)
                        .contains("Successfully published")
                        .contains(USER_ID));
    }

    @Test
    void shouldThrowExceptionOnExecutionFailure() {
        // given
        var rootCause = new RuntimeException("Kafka down");
        given(kafkaTemplate.send((ProducerRecord<String, UserLifecycleEvent>) any())).willReturn(CompletableFuture.failedFuture(rootCause));

        // when/then
        assertThatThrownBy(() -> producer.sendPasswordResetRequestedEvent(USER_ID, EMAIL, RESET_URL))
                .isInstanceOf(KafkaEventSendingException.class)
                .hasMessageContaining("Failed to send PasswordReset event");

        assertThat(logCaptor.getErrorLogs())
                .anySatisfy(log -> assertThat(log).contains(USER_ID, "Kafka down"));
    }

    @Test
    void shouldHandleInterruptionDuringSend() throws Exception {
        // given
        // We must mock the Future interface to simulate an InterruptedException on .get()
        CompletableFuture<SendResult<String, UserLifecycleEvent>> future = mock(CompletableFuture.class);
        given(kafkaTemplate.send((ProducerRecord<String, UserLifecycleEvent>) any())).willReturn(future);
        given(future.get()).willThrow(new InterruptedException("Simulated interruption"));

        // when/then
        assertThatThrownBy(() -> producer.sendPasswordResetRequestedEvent(USER_ID, EMAIL, RESET_URL))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Interrupted while sending PasswordReset event.")
                .hasCauseInstanceOf(InterruptedException.class);

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted();
    }

    @Test
    void shouldThrowExceptionWhenInputsAreNull() {
        assertThatThrownBy(() -> producer.sendPasswordResetRequestedEvent(null, EMAIL, RESET_URL))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(kafkaTemplate);
    }

    private void mockSuccessfulSend() {
        SendResult<String, UserLifecycleEvent> sendResult = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);
        given(sendResult.getRecordMetadata()).willReturn(metadata);

        given(kafkaTemplate.send((ProducerRecord<String, UserLifecycleEvent>) any())).willReturn(CompletableFuture.completedFuture(sendResult));
    }
}