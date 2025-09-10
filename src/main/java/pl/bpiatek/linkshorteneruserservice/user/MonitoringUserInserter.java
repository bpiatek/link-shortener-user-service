package pl.bpiatek.linkshorteneruserservice.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

@Component
class MonitoringUserInserter {

    private static final Logger log = LoggerFactory.getLogger(MonitoringUserInserter.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Value("${monitoring.user.name}")
    private String monitoringUsername;

    @Value("${monitoring.user.password}")
    private String monitoringPassword;


    MonitoringUserInserter(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    void seedData() {
        if (userRepository.findByEmail(monitoringUsername).isEmpty()) {
            log.info("Monitoring user not found. Creating new monitoring user...");

            User monitoringUser = new User(
                    null,
                    monitoringUsername,
                    passwordEncoder.encode(monitoringPassword),
                    List.of("ROLE_MONITORING", "ROLE_ADMIN"),
                    true,
                    clock.instant()
            );

            userRepository.save(monitoringUser);
            log.info("Monitoring user created successfully.");
        } else {
            log.info("Monitoring user already exists. No action needed.");
        }
    }
}
