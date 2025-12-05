package pl.bpiatek.linkshorteneruserservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

      var validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationError(
                        fieldError.getField(),
                        fieldError.getRejectedValue(),
                        fieldError.getDefaultMessage()
                ))
                .collect(Collectors.toList());

        log.warn("Validation failed for request on [{}]: {}", request.getRequestURI(), validationErrors);

        var apiError = new ApiError(
                "/errors/validation-error",
                "Validation Failed",
                BAD_REQUEST.value(),
                "One or more fields did not pass validation.",
                request.getRequestURI(),
                validationErrors
        );

        return new ResponseEntity<>(apiError, BAD_REQUEST);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleUserAlreadyExists(
            UserAlreadyExistsException ex,
            HttpServletRequest request) {

        var apiError = new ApiError(
                "/errors/user-already-exists",
                "User Registration Failed",
                CONFLICT.value(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );

        log.warn("User registration failed: {}", ex.getMessage());

        return new ResponseEntity<>(apiError, CONFLICT);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        log.error("Unsupported media type on request [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        var apiError = new ApiError(
                "/errors/unsupported-media-type",
                "Unsupported Media Type",
                BAD_REQUEST.value(),
                "The provided media type is not supported.",
                request.getRequestURI(),
                null
        );

        return  new ResponseEntity<>(apiError, BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception occurred on request [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        var apiError = new ApiError(
                "/errors/internal-server-error",
                "Internal Server Error",
                INTERNAL_SERVER_ERROR.value(),
                "An unexpected internal error occurred. Please try again later.",
                request.getRequestURI(),
                null
        );

        return new ResponseEntity<>(apiError, INTERNAL_SERVER_ERROR);
    }
}
