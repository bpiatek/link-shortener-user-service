package pl.bpiatek.linkshorteneruserservice.user;

import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshorteneruserservice.api.dto.LoginResponse;
import pl.bpiatek.linkshorteneruserservice.dto.UserDto;

import java.security.interfaces.RSAPublicKey;
import java.util.Optional;

public class UserFacade {

    private static final Logger log = LoggerFactory.getLogger(UserFacade.class);

    private final RegistrationService registrationService;
    private final LoginService loginService;
    private final JwtKeyProvider jwtKeyProvider;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;



    UserFacade(RegistrationService registrationService, LoginService loginService, JwtKeyProvider jwtKeyProvider,
               UserRepository userRepository, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.registrationService = registrationService;
        this.loginService = loginService;
        this.jwtKeyProvider = jwtKeyProvider;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }


    @Transactional
    public void register(String email, String password) {
        log.info("Registering user with email: {}", email);
        registrationService.register(email, password);
    }

    public LoginResponse login(String email, String password) {
        log.info("Logging in user with email: {}", email);
        return loginService.login(email, password);
    }

    public RSAPublicKey getPublicKey() {
        return jwtKeyProvider.getPublicKey();
    }

    public void verifyUser(Long userId) {
        log.info("Verifying user with ID: {}", userId);
        userRepository.verifyUser(userId);
    }

    public Optional<UserDto> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(user -> new UserDto(user.id().toString(), user.email()));
    }

    public Optional<UserDto> findById(String id) {
        return userRepository.findById(id)
                .map(user -> new UserDto(user.id().toString(), user.email()));
    }

    public void updateUserPassword(Long userId, String newPassword) {
        userRepository.updatePassword(userId, newPassword);
    }

    @Transactional
    public LoginResponse refresh(String rawRefreshToken) {
        long userId = refreshTokenService.validateAndRotate(rawRefreshToken);

        var user = userRepository.findById(String.valueOf(userId))
                .orElseThrow(() -> new RuntimeException("User not found during refresh"));

        return jwtService.generateTokensForUser(user);
    }

    @Transactional
    public void logoutEverywhere() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        var userIdString = authentication.getName();
        var userId = Long.parseLong(userIdString);

        refreshTokenService.revokeAllTokensForUser(userId);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }
}
