package pl.bpiatek.linkshorteneruserservice.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshorteneruserservice.api.dto.LoginResponse;
import pl.bpiatek.linkshorteneruserservice.exception.UserAlreadyExistsException;

import java.time.Clock;
import java.util.List;

@Service
class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final  UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    void register(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException(email);
        }

        var passwordHash = passwordEncoder.encode(password);
        var user = new User(null, email, passwordHash, List.of("ROLE_USER"), false, clock.instant());

        var savedUser = userRepository.save(user);
        log.info("Successfully registered user with ID: {}", savedUser.id());
    }

    LoginResponse login(String email, String password) {
        var authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        return jwtService.generateTokens(authentication);
    }
}
