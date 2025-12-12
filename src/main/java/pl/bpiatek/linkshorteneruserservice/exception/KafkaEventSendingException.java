package pl.bpiatek.linkshorteneruserservice.exception;

public class KafkaEventSendingException extends RuntimeException {

    public KafkaEventSendingException(String message) {
        super(message);
    }
}
