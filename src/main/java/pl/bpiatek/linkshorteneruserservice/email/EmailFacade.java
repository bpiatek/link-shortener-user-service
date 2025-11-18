package pl.bpiatek.linkshorteneruserservice.email;

import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshorteneruserservice.user.UserFacade;

public class EmailFacade {

    private final TokenVerificationService tokenVerificationService;
    private final UserFacade userFacade;

    EmailFacade(TokenVerificationService tokenVerificationService, UserFacade userFacade) {
        this.tokenVerificationService = tokenVerificationService;
        this.userFacade = userFacade;
    }

    public String generateAndSaveToken(Long userId, String email) {
        return tokenVerificationService.generateAndSaveToken(userId, email);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        var userId = tokenVerificationService.verifyToken(rawToken);
        userFacade.verifyUser(userId);
    }
}
