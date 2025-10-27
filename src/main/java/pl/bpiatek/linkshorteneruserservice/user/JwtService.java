package pl.bpiatek.linkshorteneruserservice.user;

import io.jsonwebtoken.Jwts;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pl.bpiatek.linkshorteneruserservice.api.dto.LoginResponse;

import java.time.Clock;
import java.util.Date;

class JwtService {

    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final Clock clock;
    private final UserRepository userRepository;
    private final JwtKeyProvider jwtKeyProvider;

    public JwtService(long accessTokenExpiration, long refreshTokenExpiration, Clock clock,
                      UserRepository userRepository, JwtKeyProvider jwtKeyProvider) {
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.clock = clock;
        this.userRepository = userRepository;
        this.jwtKeyProvider = jwtKeyProvider;
    }

    LoginResponse generateTokens(Authentication authentication) {
        var email = authentication.getName();

        var user = userRepository.findByEmail(email)
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
        var now = Date.from(clock.instant());
        var expirationDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(user.id().toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(jwtKeyProvider.getPrivateKey())
                .compact();
    }
}
