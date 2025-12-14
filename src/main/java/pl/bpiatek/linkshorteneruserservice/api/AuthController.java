package pl.bpiatek.linkshorteneruserservice.api;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.bpiatek.linkshorteneruserservice.api.dto.ForgotPasswordRequest;
import pl.bpiatek.linkshorteneruserservice.api.dto.LoginRequest;
import pl.bpiatek.linkshorteneruserservice.api.dto.LoginResponse;
import pl.bpiatek.linkshorteneruserservice.api.dto.RegisterRequest;
import pl.bpiatek.linkshorteneruserservice.api.dto.ResetPasswordRequest;
import pl.bpiatek.linkshorteneruserservice.email.EmailFacade;
import pl.bpiatek.linkshorteneruserservice.exception.ExpiredTokenException;
import pl.bpiatek.linkshorteneruserservice.exception.InvalidTokenException;
import pl.bpiatek.linkshorteneruserservice.password.PasswordResetTokenFacade;
import pl.bpiatek.linkshorteneruserservice.user.UserFacade;

import java.net.URI;

import static org.springframework.http.HttpStatus.CREATED;


@RestController
@RequestMapping("/auth")
class AuthController {

    private final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserFacade userFacade;
    private final EmailFacade emailFacade;
    private final PasswordResetTokenFacade passwordResetTokenFacade;

    private AuthController(UserFacade userFacade,
                           EmailFacade emailFacade,
                           PasswordResetTokenFacade passwordResetTokenFacade) {
        this.userFacade = userFacade;
        this.emailFacade = emailFacade;
        this.passwordResetTokenFacade = passwordResetTokenFacade;
    }

    @PostMapping("/register")
    ResponseEntity<String> registerUser(@Valid @RequestBody RegisterRequest request) {
        log.info("Registering user: {}", request.email());
        userFacade.register(request.email(), request.password());
        log.info("User registered: {}", request.email());
        return ResponseEntity.status(CREATED).body("User registered successfully.");
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        log.info("Logging in user: {}", request.email());
        var tokens = userFacade.login(request.email(), request.password());
        return ResponseEntity.ok(tokens);
    }

    @GetMapping("/verify")
    ResponseEntity<Void> verifyEmail(@RequestParam("token") String token) {
        log.info("Verifying email with token: {}", token);

        try {
            emailFacade.verifyEmail(token);
            return ResponseEntity.ok().build();
        } catch (InvalidTokenException | ExpiredTokenException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/forgot-password")
    ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetTokenFacade.requestPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetTokenFacade.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
