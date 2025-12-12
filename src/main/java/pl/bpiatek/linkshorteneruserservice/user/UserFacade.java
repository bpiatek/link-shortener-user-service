package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshorteneruserservice.api.dto.LoginResponse;
import pl.bpiatek.linkshorteneruserservice.dto.UserDto;

import java.security.interfaces.RSAPublicKey;
import java.util.Optional;

public class UserFacade {

    private final RegistrationService registrationService;
    private final LoginService loginService;
    private final JwtKeyProvider jwtKeyProvider;
    private final UserRepository userRepository;

    UserFacade(RegistrationService registrationService, LoginService loginService, JwtKeyProvider jwtKeyProvider, UserRepository userRepository) {
        this.registrationService = registrationService;
        this.loginService = loginService;
        this.jwtKeyProvider = jwtKeyProvider;
        this.userRepository = userRepository;
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

    public void verifyUser(Long userId) {
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
}
