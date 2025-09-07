package pl.bpiatek.linkshorteneruserservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

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

        ApiError apiError = new ApiError(
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
}
