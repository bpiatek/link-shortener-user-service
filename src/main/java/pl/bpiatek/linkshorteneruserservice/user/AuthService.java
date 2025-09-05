package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pl.bpiatek.linkshorteneruserservice.exception.UserAlreadyExistsException;

import java.time.Clock;

@Service
class AuthService {

    private final  UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final Clock clock;

    AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.clock = clock;
    }

    void register(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException("User with email " + email + " already exists.");
        }

        var passwordHash = passwordEncoder.encode(password);
        var user = new User(null, email, passwordHash, "ROLE_USER", false, clock.instant());

        userRepository.save(user);
    }


}
