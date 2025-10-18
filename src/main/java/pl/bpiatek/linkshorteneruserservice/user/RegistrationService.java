package pl.bpiatek.linkshorteneruserservice.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshorteneruserservice.exception.UserAlreadyExistsException;

import java.time.Clock;
import java.util.List;


class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;
    private final VerificationTokenService verificationTokenService;

    RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock, ApplicationEventPublisher eventPublisher, VerificationTokenService verificationTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
        this.verificationTokenService = verificationTokenService;
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

        var rawToken = verificationTokenService.generateAndSaveToken(savedUser);
        log.info("Saved verification token for user ID: {}", savedUser.id());

        var event = new UserRegisteredApplicationEvent(
                String.valueOf(savedUser.id()),
                savedUser.email(),
                rawToken
        );

        eventPublisher.publishEvent(event);
        log.info("Published UserRegisteredApplicationEvent for user ID: {}", savedUser.id());
    }
}
