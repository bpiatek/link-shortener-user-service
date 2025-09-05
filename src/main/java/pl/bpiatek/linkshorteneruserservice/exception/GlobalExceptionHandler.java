package pl.bpiatek.linkshorteneruserservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestControllerAdvice
class GlobalExceptionHandler {

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
}
