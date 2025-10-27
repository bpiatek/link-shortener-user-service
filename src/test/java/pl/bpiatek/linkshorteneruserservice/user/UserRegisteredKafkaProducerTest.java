package pl.bpiatek.linkshorteneruserservice.user;

import nl.altindag.log.LogCaptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.assertj.core.api.SoftAssertions;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserRegisteredKafkaProducerTest {

    private static final String TEST_TOPIC = "test-topic";
    private static final String USER_ID = "1";
    private static final String EMAIL = "test@example.com";
    private static final String TOKEN = "token";

    @Mock
    private KafkaTemplate<String, UserLifecycleEvent> kafkaTemplate;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, UserLifecycleEvent>> producerRecordCaptor;

    private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneOffset.UTC);
    private LogCaptor logCaptor;


    private UserRegisteredKafkaProducer userRegisteredKafkaProducer;

    @BeforeEach
    void setUp() {
        userRegisteredKafkaProducer = new UserRegisteredKafkaProducer(kafkaTemplate, TEST_TOPIC, clock);
        logCaptor = LogCaptor.forClass(UserRegisteredKafkaProducer.class);
        SendResult<String, UserLifecycleEvent> mockSendResult = mock(SendResult.class);
        var mockRecordMetadata = mock(RecordMetadata.class);

        lenient().when(mockSendResult.getRecordMetadata()).thenReturn(mockRecordMetadata);
        lenient().when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(mockSendResult));
    }

    @Test
    void shouldSendMessage() {
        // when
        userRegisteredKafkaProducer.sendUserRegisteredEvent(USER_ID, EMAIL, TOKEN);

        // then
        verify(kafkaTemplate).send(producerRecordCaptor.capture());
        var sentRecord = producerRecordCaptor.getValue();

        var softly = new SoftAssertions();
        assertRecordBasics(sentRecord, softly);
        assertHeaders(sentRecord, softly);
        softly.assertAll();
    }
    
    @Test
    void shouldLogInfoOnSendingMessage() {
        // given
        SendResult<String, UserLifecycleEvent> mockSendResult = mock(SendResult.class);
        var mockRecordMetadata = mock(RecordMetadata.class);
        given(mockSendResult.getRecordMetadata()).willReturn(mockRecordMetadata);
        given(mockRecordMetadata.partition()).willReturn(1);
        given(mockRecordMetadata.offset()).willReturn(123L);

        given(kafkaTemplate.send(any(ProducerRecord.class)))
                .willReturn(CompletableFuture.completedFuture(mockSendResult));

        // when
        userRegisteredKafkaProducer.sendUserRegisteredEvent(USER_ID, EMAIL, TOKEN);

        // then
        assertThat(logCaptor.getInfoLogs()).hasSize(2);
        assertThat(logCaptor.getInfoLogs().get(1))
                .isEqualTo("Successfully published UserRegistered event for userId: 1 to partition: 1 offset: 123");
    }
    
    @Test
    void shouldLogErrorOnException() {
        // given
        var errorMessage = "Kafka broker is not available";
        var exception = new ExecutionException(errorMessage, new RuntimeException());

        given(kafkaTemplate.send(any(ProducerRecord.class)))
                .willReturn(CompletableFuture.failedFuture(exception));

        // when
        userRegisteredKafkaProducer.sendUserRegisteredEvent(USER_ID, EMAIL, TOKEN);

        // then
        assertThat(logCaptor.getErrorLogs().size()).isOne();
        assertThat(logCaptor.getErrorLogs().getFirst())
                .isEqualTo("Failed to publish UserRegistered event for userId: 1. Reason: " + errorMessage);
    }

    private void assertRecordBasics(ProducerRecord<String, UserLifecycleEvent> record, SoftAssertions softly) {
        softly.assertThat(record.topic()).isEqualTo(TEST_TOPIC);
        softly.assertThat(record.key()).isNotNull();
        softly.assertThat(record.value().getUserRegistered().getUserId()).isEqualTo(USER_ID);
    }

    private void assertHeaders(ProducerRecord<String, UserLifecycleEvent> record, SoftAssertions softly) {
        var traceId = record.headers().lastHeader("trace-id");
        softly.assertThat(traceId).isNotNull();
        softly.assertThat(new String(traceId.value(), UTF_8)).isNotEmpty();

        var source = record.headers().lastHeader("source");
        softly.assertThat(source).isNotNull();
        softly.assertThat(new String(source.value(), UTF_8)).isEqualTo("user-service");
    }
}