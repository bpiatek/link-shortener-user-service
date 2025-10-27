package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

class JdbcEmailVerificationRepository implements  EmailVerificationRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final Clock clock;
    private final EmailVerificationRowMapper rowMapper = new EmailVerificationRowMapper();

    JdbcEmailVerificationRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.clock = clock;
    }

    @Override
    public void save(long userId, String tokenHash, Instant expiresAt) {
        // This "upsert" query is atomic and highly efficient.
        // It handles both creating a new token and replacing an existing one for a user.
        var sql = """
            INSERT INTO email_verifications (user_id, token_hash, expires_at, created_at)
            VALUES (:userId, :tokenHash, :expiresAt, :createdAt)
            ON CONFLICT (user_id) DO UPDATE SET
              token_hash = :tokenHash,
              expires_at = :expiresAt,
              created_at = :createdAt
            """;

        var params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("tokenHash", tokenHash)
                .addValue("expiresAt", Timestamp.from(expiresAt))
                .addValue("createdAt", Timestamp.from(clock.instant()));

        namedJdbcTemplate.update(sql, params);
    }

    @Override
    public Optional<EmailVerification> findByTokenHash(String tokenHash) {
        var sql = "SELECT * FROM email_verifications WHERE token_hash = :tokenHash";
        var params = new MapSqlParameterSource().addValue("tokenHash", tokenHash);

        try {
            var verification = namedJdbcTemplate.queryForObject(sql, params, rowMapper);
            return Optional.ofNullable(verification);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void deleteByUserId(long userId) {
        var sql = "DELETE FROM email_verifications WHERE user_id = :userId";
        var params = new MapSqlParameterSource().addValue("userId", userId);
        namedJdbcTemplate.update(sql, params);
    }
}
