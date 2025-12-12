package pl.bpiatek.linkshorteneruserservice.password;

import nl.altindag.log.LogCaptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.assertj.core.api.SoftAssertions;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordChangedKafkaProducerTest {

    private static final String TEST_TOPIC = "test-topic";
    private static final String USER_ID = "1";
    private static final String EMAIL = "test@example.com";

    @Mock
    private KafkaTemplate<String, UserLifecycleEvent> kafkaTemplate;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, UserLifecycleEvent>> producerRecordCaptor;

    private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneOffset.UTC);
    private LogCaptor logCaptor;

    private PasswordChangedKafkaProducer producer;

    @BeforeEach
    void setUp() {
        producer = new PasswordChangedKafkaProducer(kafkaTemplate, TEST_TOPIC, clock);
        logCaptor = LogCaptor.forClass(PasswordChangedKafkaProducer.class);
    }

    @AfterEach
    void tearDown() {
        logCaptor.clearLogs();
    }

    @Test
    void shouldSendMessageWithCorrectData() {
        mockSuccessfulSend();

        producer.sendPasswordChangedEvent(USER_ID, EMAIL);

        verify(kafkaTemplate).send(producerRecordCaptor.capture());
        var record = producerRecordCaptor.getValue();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(record.topic()).isEqualTo(TEST_TOPIC);
            softly.assertThat(record.key()).isEqualTo(USER_ID);

            var payload = record.value().getUserPasswordChanged();
            softly.assertThat(payload.getUserId()).isEqualTo(USER_ID);
            softly.assertThat(payload.getEmail()).isEqualTo(EMAIL);
        });
    }

    @Test
    void shouldLogInfoOnAsyncSuccess() {
        mockSuccessfulSend();

        producer.sendPasswordChangedEvent(USER_ID, EMAIL);

        assertThat(logCaptor.getInfoLogs())
                .anySatisfy(log -> assertThat(log).contains("Successfully published", USER_ID));
    }

    @Test
    void shouldLogAndThrowErrorOnExecutionFailure() {
        // given
        var exceptionMessage = "Kafka connection refused";
        given(kafkaTemplate.send((ProducerRecord<String, UserLifecycleEvent>) any()))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException(exceptionMessage)));

        // when/then
        assertThatThrownBy(() -> producer.sendPasswordChangedEvent(USER_ID, EMAIL))
                .isInstanceOf(KafkaEventSendingException.class)
                        .hasMessageContaining("Failed to send PasswordChanged event.");

        assertThat(logCaptor.getErrorLogs())
                .hasSize(1)
                .first()
                .satisfies(log -> {
                    assertThat(log).contains("Failed to publish PasswordChanged event");
                    assertThat(log).contains(USER_ID);
                    assertThat(log).contains(exceptionMessage);
                });
    }

    @Test
    void shouldHandleInterruption() throws Exception {
        // given
        CompletableFuture<SendResult<String, UserLifecycleEvent>> future = mock(CompletableFuture.class);
        given(kafkaTemplate.send((ProducerRecord<String, UserLifecycleEvent>) any())).willReturn(future);
        given(future.get()).willThrow(new InterruptedException("Simulated interruption"));

        // when/then
        assertThatThrownBy(() -> producer.sendPasswordChangedEvent(USER_ID, EMAIL))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(InterruptedException.class);

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted();
    }

    private void mockSuccessfulSend() {
        SendResult<String, UserLifecycleEvent> sendResult = mock(SendResult.class);
        given(sendResult.getRecordMetadata()).willReturn(mock(RecordMetadata.class));
        given(kafkaTemplate.send((ProducerRecord<String, UserLifecycleEvent>) any()))
                .willReturn(CompletableFuture.completedFuture(sendResult));
    }
}