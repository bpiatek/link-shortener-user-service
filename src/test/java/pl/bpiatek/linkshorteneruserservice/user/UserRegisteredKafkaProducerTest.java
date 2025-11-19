package pl.bpiatek.linkshorteneruserservice.user;

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

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserRegisteredKafkaProducerTest {

    private static final String TEST_TOPIC = "test-topic";
    private static final String USER_ID = "1";
    private static final String EMAIL = "test@example.com";
    private static final String VERIFICATION_URL = "https://app.com/verify?token=abc";


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
    }

    @AfterEach
    void tearDown() {
        logCaptor.clearLogs();
    }

    @Test
    void shouldSendMessageWithCorrectData() {
        // given
        SendResult<String, UserLifecycleEvent> mockSendResult = mock(SendResult.class);
        given(mockSendResult.getRecordMetadata()).willReturn(mock(RecordMetadata.class));

        given(kafkaTemplate.send(any(ProducerRecord.class)))
                .willReturn(CompletableFuture.completedFuture(mockSendResult));

        // when
        userRegisteredKafkaProducer.sendUserRegisteredEvent(USER_ID, EMAIL, VERIFICATION_URL);

        // then
        verify(kafkaTemplate).send(producerRecordCaptor.capture());
        var sentRecord = producerRecordCaptor.getValue();

        var softly = new SoftAssertions();
        assertRecordBasics(sentRecord, softly);
        assertHeaders(sentRecord, softly);
        softly.assertAll();
    }

    @Test
    void shouldLogInfoOnAsyncSuccess() {
        // given
        SendResult<String, UserLifecycleEvent> mockSendResult = mock(SendResult.class);
        var mockRecordMetadata = mock(RecordMetadata.class);
        given(mockSendResult.getRecordMetadata()).willReturn(mockRecordMetadata);
        given(mockRecordMetadata.partition()).willReturn(1);
        given(mockRecordMetadata.offset()).willReturn(123L);

        given(kafkaTemplate.send(any(ProducerRecord.class)))
                .willReturn(CompletableFuture.completedFuture(mockSendResult));

        // when
        userRegisteredKafkaProducer.sendUserRegisteredEvent(USER_ID, EMAIL, VERIFICATION_URL);

        // then
        assertThat(logCaptor.getInfoLogs()).hasSize(2);
        assertThat(logCaptor.getInfoLogs().get(1))
                .isEqualTo("Successfully published UserRegistered event for userId: 1 to partition: 1 offset: 123");
    }

    @Test
    void shouldLogErrorOnAsyncFailure() {
        // given
        var errorMessage = "Kafka broker is not available";
        var exception = new RuntimeException(errorMessage);

        given(kafkaTemplate.send(any(ProducerRecord.class)))
                .willReturn(CompletableFuture.failedFuture(exception));

        // when
        userRegisteredKafkaProducer.sendUserRegisteredEvent(USER_ID, EMAIL, VERIFICATION_URL);

        // then
        assertThat(logCaptor.getErrorLogs()).hasSize(1);
        assertThat(logCaptor.getErrorLogs().getFirst())
                .contains("Failed to publish UserRegistered event for userId: 1")
                .contains("Reason: " + errorMessage);
    }

    @Test
    void shouldSetCorrectEventTimestamp() {
        // given
        SendResult<String, UserLifecycleEvent> mockSendResult = mock(SendResult.class);
        given(mockSendResult.getRecordMetadata()).willReturn(mock(RecordMetadata.class));
        given(kafkaTemplate.send(any(ProducerRecord.class)))
                .willReturn(CompletableFuture.completedFuture(mockSendResult));

        // when
        userRegisteredKafkaProducer.sendUserRegisteredEvent(USER_ID, EMAIL, VERIFICATION_URL);

        // then
        verify(kafkaTemplate).send(producerRecordCaptor.capture());
        var sentRecord = producerRecordCaptor.getValue();
        var timestamp = sentRecord.value().getEventTimestamp();

        var expectedInstant = clock.instant();
        assertThat(timestamp.getSeconds()).isEqualTo(expectedInstant.getEpochSecond());
        assertThat(timestamp.getNanos()).isEqualTo(expectedInstant.getNano());
    }

    @Test
    void shouldGenerateDeterministicKeyForSameInputs() {
        // given
        SendResult<String, UserLifecycleEvent> mockSendResult = mock(SendResult.class);
        given(mockSendResult.getRecordMetadata()).willReturn(mock(RecordMetadata.class));
        given(kafkaTemplate.send(any(ProducerRecord.class)))
                .willReturn(CompletableFuture.completedFuture(mockSendResult));

        // when
        userRegisteredKafkaProducer.sendUserRegisteredEvent(USER_ID, EMAIL, VERIFICATION_URL);
        userRegisteredKafkaProducer.sendUserRegisteredEvent(USER_ID, EMAIL, VERIFICATION_URL);

        // then
        verify(kafkaTemplate, org.mockito.Mockito.times(2)).send(producerRecordCaptor.capture());
        var allRecords = producerRecordCaptor.getAllValues();

        assertThat(allRecords.get(0).key()).isEqualTo(allRecords.get(1).key());
        assertThat(allRecords.get(0).value().getEventId()).isEqualTo(allRecords.get(1).value().getEventId());
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsNull() {
        // when then
        assertThatThrownBy(() ->
                userRegisteredKafkaProducer.sendUserRegisteredEvent(null, EMAIL, VERIFICATION_URL))
                .isInstanceOf(NullPointerException.class);

        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    void shouldThrowExceptionWhenEmailIsNull() {
        // when then
        assertThatThrownBy(() ->
                userRegisteredKafkaProducer.sendUserRegisteredEvent(USER_ID, null, VERIFICATION_URL))
                .isInstanceOf(NullPointerException.class);

        verify(kafkaTemplate, never()).send(any(), any());
    }

    private void assertRecordBasics(ProducerRecord<String, UserLifecycleEvent> record, SoftAssertions softly) {
        softly.assertThat(record.topic()).isEqualTo(TEST_TOPIC);

        var expectedSignature = USER_ID + EMAIL;
        var expectedId = UUID.nameUUIDFromBytes(expectedSignature.getBytes(StandardCharsets.UTF_8)).toString();

        softly.assertThat(record.key()).as("Kafka Record Key").isEqualTo(expectedId);
        softly.assertThat(record.value().getEventId()).as("Payload Event ID").isEqualTo(expectedId);

        var payload = record.value().getUserRegistered();
        softly.assertThat(payload.getUserId()).isEqualTo(USER_ID);
        softly.assertThat(payload.getEmail()).isEqualTo(EMAIL);
        softly.assertThat(payload.getVerificationUrl()).isEqualTo(VERIFICATION_URL);
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