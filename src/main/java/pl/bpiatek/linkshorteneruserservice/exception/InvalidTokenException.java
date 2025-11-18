package pl.bpiatek.linkshorteneruserservice.exception;


public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
