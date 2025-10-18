package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import pl.bpiatek.linkshorteneruserservice.api.dto.LoginResponse;

class LoginService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    LoginService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    LoginResponse login(String email, String password) {
        var authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        return jwtService.generateTokens(authentication);
    }
}
