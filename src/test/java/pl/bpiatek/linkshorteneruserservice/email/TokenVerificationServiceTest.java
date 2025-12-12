package pl.bpiatek.linkshorteneruserservice.email;

import com.google.common.hash.Hashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.bpiatek.linkshorteneruserservice.exception.ExpiredTokenException;
import pl.bpiatek.linkshorteneruserservice.exception.InvalidTokenException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenVerificationServiceTest {

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    private TokenVerificationService service;

    private static final Instant NOW = Instant.parse("2024-01-01T12:00:00Z");
    private static final long EXPIRATION_SEC = 3600; // 1 hour

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(NOW, ZoneId.of("UTC"));
        service = new TokenVerificationService(emailVerificationRepository, clock, EXPIRATION_SEC);
    }

    @Test
    void shouldGenerateAndSaveToken() {
        // given
        var userId = 100L;
        var email = "test@example.com";

        // when
        var generatedToken = service.generateAndSaveToken(userId, email);

        // then
        var hashCaptor = ArgumentCaptor.forClass(String.class);
        var timeCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(emailVerificationRepository).save(eq(userId), hashCaptor.capture(), timeCaptor.capture());

        assertThat(hashCaptor.getValue()).isEqualTo(hash(generatedToken));
        assertThat(timeCaptor.getValue()).isEqualTo(NOW.plusSeconds(EXPIRATION_SEC));
    }

    @Test
    void shouldVerifyTokenSuccessfully() {
        // given
        var rawToken = "valid-token";
        var userId = 100L;
        given(emailVerificationRepository.findByTokenHash(hash(rawToken)))
                .willReturn(Optional.of(validVerification(userId, rawToken)));

        // when
        var resultUserId = service.verifyToken(rawToken);

        // then
        assertThat(resultUserId).isEqualTo(userId);
        verify(emailVerificationRepository).deleteByUserId(userId);
    }

    @Test
    void shouldThrowWhenTokenNotFound() {
        // given
        var rawToken = "unknown-token";

        given(emailVerificationRepository.findByTokenHash(hash(rawToken)))
                .willReturn(Optional.empty());

        // when then
        assertThatThrownBy(() -> service.verifyToken(rawToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid verification token.");
    }

    @Test
    void shouldThrowWhenTokenExpired() {
        // given
        var rawToken = "expired-token";
        var expiredVerification = new EmailVerification(
                1L, 100L, hash(rawToken),
                NOW.minusSeconds(1), // Expired 1 second ago
                NOW.minusSeconds(3600)
        );

        given(emailVerificationRepository.findByTokenHash(hash(rawToken)))
                .willReturn(Optional.of(expiredVerification));

        // when then
        assertThatThrownBy(() -> service.verifyToken(rawToken))
                .isInstanceOf(ExpiredTokenException.class)
                .hasMessage("Verification token has expired.");
    }

    private String hash(String rawToken) {
        return Hashing.sha256()
                .hashString(rawToken, UTF_8)
                .toString();
    }

    private EmailVerification validVerification(Long userId, String rawToken) {
        return new EmailVerification(
                1L,
                userId,
                hash(rawToken),
                NOW.plusSeconds(EXPIRATION_SEC),
                NOW
        );
    }
}