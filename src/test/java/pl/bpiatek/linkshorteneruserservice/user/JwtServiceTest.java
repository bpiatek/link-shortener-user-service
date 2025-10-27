package pl.bpiatek.linkshorteneruserservice.user;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtKeyProvider jwtKeyProvider;

    private JwtService jwtService;
    private PublicKey publicKey;

    private static final long ACCESS_TOKEN_EXPIRATION_MS = 3_600_000;
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 86_400_000;
    private static final Instant NOW = Instant.parse("2025-10-26T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private User testUser;
    private Authentication mockAuthentication;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        // Generate a real RSA key pair for testing
        var kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var keyPair = kpg.generateKeyPair();
        publicKey = keyPair.getPublic();

        lenient().when(jwtKeyProvider.getPrivateKey()).thenReturn((RSAPrivateKey) keyPair.getPrivate());

        jwtService = new JwtService(
                ACCESS_TOKEN_EXPIRATION_MS,
                REFRESH_TOKEN_EXPIRATION_MS,
                FIXED_CLOCK,
                userRepository,
                jwtKeyProvider
        );

        testUser = new User(123L, "test@example.com", "password", List.of("ROLE_USER"), true, NOW);
        mockAuthentication = mock(Authentication.class);
        given(mockAuthentication.getName()).willReturn(testUser.email());
        given(userRepository.findByEmail(testUser.email())).willReturn(Optional.of(testUser));
    }

    @Test
    void shouldGenerateValidAccessTokenWithCorrectClaims() {
        // when
        var accessToken = jwtService.generateTokens(mockAuthentication).accessToken();

        // then
        assertThat(accessToken).isNotBlank();
        assertAccessTokenClaims(accessToken, testUser);
    }

    @Test
    void shouldGenerateValidRefreshTokenWithCorrectClaims() {
        // when
        var refreshToken = jwtService.generateTokens(mockAuthentication).refreshToken();

        // then
        assertThat(refreshToken).isNotBlank();
        assertRefreshTokenClaims(refreshToken, testUser);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // given
        given(userRepository.findByEmail(testUser.email())).willReturn(Optional.empty());

        // when then
        assertThatThrownBy(() -> jwtService.generateTokens(mockAuthentication))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found after authentication: " + testUser.email());
    }

    private void assertAccessTokenClaims(String token, User user) {
        var claims = parseToken(token);

        assertSoftly(softly -> {
            softly.assertThat(claims.getSubject()).isEqualTo(user.id().toString());
            softly.assertThat(claims.get("email", String.class)).isEqualTo(user.email());
            softly.assertThat(claims.get("roles", List.class)).containsExactlyElementsOf(user.roles());
            softly.assertThat(claims.getIssuedAt()).isEqualTo(Date.from(NOW));
            softly.assertThat(claims.getExpiration()).isEqualTo(Date.from(NOW.plusMillis(ACCESS_TOKEN_EXPIRATION_MS)));
        });
    }

    private void assertRefreshTokenClaims(String token, User user) {
        var claims = parseToken(token);

        assertSoftly(softly -> {
            softly.assertThat(claims.getSubject()).isEqualTo(user.id().toString());
            softly.assertThat(claims.get("type", String.class)).isEqualTo("refresh");
            softly.assertThat(claims.getIssuedAt()).isEqualTo(Date.from(NOW));
            softly.assertThat(claims.getExpiration()).isEqualTo(Date.from(NOW.plusMillis(REFRESH_TOKEN_EXPIRATION_MS)));
        });
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}