package pl.bpiatek.linkshorteneruserservice.user;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pl.bpiatek.linkshorteneruserservice.api.dto.LoginResponse;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.util.Date;

@Service
class JwtService {

    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final Clock clock;
    private final UserRepository userRepository;
    private final JwtKeyProvider jwtKeyProvider;

    public JwtService(
            @Value("${app.jwt.access-token.expiration}") long accessTokenExpiration,
            @Value("${app.jwt.refresh-token.expiration}") long refreshTokenExpiration, Clock clock, UserRepository userRepository, JwtKeyProvider jwtKeyProvider) {
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.clock = clock;
        this.userRepository = userRepository;
        this.jwtKeyProvider = jwtKeyProvider;
    }

    LoginResponse generateTokens(Authentication authentication) {
        var email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found after authentication: " + email));

        var accessToken = generateAccessToken(user);
        var refreshToken = generateRefreshToken(user);

        return new LoginResponse(accessToken, refreshToken);
    }

    private String generateAccessToken(User user) {
        var now = Date.from(clock.instant());
        var expirationDate = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(user.id().toString())
                .issuedAt(now)
                .expiration(expirationDate)
                .claim("email", user.email())
                .claim("roles", user.roles())
                .signWith(jwtKeyProvider.getPrivateKey())
                .compact();
    }

    private String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(user.id().toString()) // Use user ID in refresh token too
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtKeyProvider.getPrivateKey())
                .compact();
    }

}
