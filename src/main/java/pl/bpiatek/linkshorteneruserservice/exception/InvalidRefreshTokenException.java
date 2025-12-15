package pl.bpiatek.linkshorteneruserservice.exception;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {

        super(message);
    }
}
