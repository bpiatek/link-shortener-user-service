package pl.bpiatek.linkshorteneruserservice.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.bpiatek.linkshorteneruserservice.api.dto.LoginRequest;
import pl.bpiatek.linkshorteneruserservice.api.dto.LoginResponse;
import pl.bpiatek.linkshorteneruserservice.api.dto.RegisterRequest;
import pl.bpiatek.linkshorteneruserservice.user.UserFacade;

import static org.springframework.http.HttpStatus.CREATED;


@RestController
@RequestMapping("/auth")
class AuthController {

    private final UserFacade userFacade;

    private AuthController(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    @PostMapping("/register")
    ResponseEntity<String> registerUser(@Valid @RequestBody RegisterRequest request) {
        userFacade.register(request.email(), request.password());
        return ResponseEntity.status(CREATED).body("User registered successfully.");
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        var tokens = userFacade.login(request.email(), request.password());
        return ResponseEntity.ok(tokens);
    }
}
