package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshorteneruserservice.api.dto.LoginResponse;

import java.security.interfaces.RSAPublicKey;

public class UserFacade {

    private final RegistrationService registrationService;
    private final LoginService loginService;
    private final JwtKeyProvider jwtKeyProvider;

    UserFacade(RegistrationService registrationService, LoginService loginService, JwtKeyProvider jwtKeyProvider) {
        this.registrationService = registrationService;
        this.loginService = loginService;
        this.jwtKeyProvider = jwtKeyProvider;
    }


    @Transactional
    public void register(String email, String password) {
        registrationService.register(email, password);
    }

    public LoginResponse login(String email, String password) {
        return loginService.login(email, password);
    }

    public RSAPublicKey getPublicKey() {
        return jwtKeyProvider.getPublicKey();
    }
}
