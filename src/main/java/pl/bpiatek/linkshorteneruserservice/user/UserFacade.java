package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.stereotype.Component;

@Component
public class UserFacade {

    private final AuthService authService;

    private UserFacade(AuthService authService) {
        this.authService = authService;
    }

    public void register(String email, String password) {
        authService.register(email, password);
    }

}
