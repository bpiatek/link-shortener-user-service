package pl.bpiatek.linkshorteneruserservice.email;

public class EmailFacade {

    private final TokenVerificationService tokenVerificationService;

    EmailFacade(TokenVerificationService tokenVerificationService) {
        this.tokenVerificationService = tokenVerificationService;
    }

    public String generateAndSaveToken(Long userId, String email) {
        return tokenVerificationService.generateAndSaveToken(userId, email);
    }

    public void verifyEmail(String rawToken) {
        tokenVerificationService.verifyToken(rawToken);
    }
}
