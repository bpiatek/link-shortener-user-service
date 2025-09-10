package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshorteneruserservice.api.dto.LoginResponse;

import java.security.interfaces.RSAPublicKey;

@Component
public class UserFacade {

    private final AuthService authService;
    private final JwtKeyProvider jwtKeyProvider;

    private UserFacade(AuthService authService, JwtKeyProvider jwtKeyProvider) {
        this.authService = authService;
        this.jwtKeyProvider = jwtKeyProvider;
    }

    @Transactional
    public void register(String email, String password) {
        authService.register(email, password);
    }

    public LoginResponse login(String email, String password) {
        return authService.login(email, password);
    }

    public RSAPublicKey getPublicKey() {
        return jwtKeyProvider.getPublicKey();
    }
}
