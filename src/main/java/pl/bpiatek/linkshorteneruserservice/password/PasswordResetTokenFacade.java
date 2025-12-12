package pl.bpiatek.linkshorteneruserservice.password;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshorteneruserservice.user.UserFacade;

public class PasswordResetTokenFacade {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetTokenFacade.class);

    private final UserFacade userFacade;
    private final PasswordResetTokenService passwordResetTokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final String appBaseUrl;
    private final String resetPasswordUrl;
    private final PasswordEncoder passwordEncoder;

    PasswordResetTokenFacade(UserFacade userFacade,
                             PasswordResetTokenService passwordResetTokenService,
                             ApplicationEventPublisher eventPublisher, String appBaseUrl,
                             String resetPasswordUrl, PasswordEncoder passwordEncoder) {
        this.userFacade = userFacade;
        this.passwordResetTokenService = passwordResetTokenService;
        this.eventPublisher = eventPublisher;
        this.appBaseUrl = appBaseUrl;
        this.resetPasswordUrl = resetPasswordUrl;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void requestPasswordReset(String email) {
        var userOptional = userFacade.findByEmail(email);
        if (userOptional.isEmpty()) {
            log.info("Reset requested for unknown email: {}", email);
            return;
        }

        var user = userOptional.get();
        var rawToken = passwordResetTokenService.generateAndSaveToken(user.id());
        var resetUrl = appBaseUrl + resetPasswordUrl + rawToken;
        var event = new PasswordResetApplicationEvent(user.id(), email, resetUrl);

        eventPublisher.publishEvent(event);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        var userId = passwordResetTokenService.validateAndConsumeToken(token);
        var user = userFacade.findById(userId.toString());
        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User with id " + userId + " not found");
        }

        var newHash = passwordEncoder.encode(newPassword);
        userFacade.updateUserPassword(userId, newHash);

        var event = new PasswordChangedApplicationEvent(userId.toString(), user.get().email());
        eventPublisher.publishEvent(event);
    }
}
