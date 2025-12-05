package pl.bpiatek.linkshorteneruserservice.password;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

class JdbcPasswordResetTokenRepository implements PasswordResetTokenRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final Clock clock;
    private final PasswordResetTokenRowMapper rowMapper = new PasswordResetTokenRowMapper();

    public JdbcPasswordResetTokenRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.clock = clock;
    }

    @Override
    public void save(Long userId, String tokenHash, Instant expiresAt) {
        var sql = """
                INSERT INTO password_reset_tokens (user_id, token_hash, expires_at, created_at)
                VALUES (:userId, :tokenHash, :expiresAt, :createdAt)
                ON CONFLICT (user_id) DO UPDATE SET
                   token_hash = EXCLUDED.token_hash,
                   expires_at = EXCLUDED.expires_at,
                   created_at = EXCLUDED.created_at""";

        var params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("tokenHash", tokenHash)
                .addValue("expiresAt", Timestamp.from(expiresAt))
                .addValue("createdAt", Timestamp.from(clock.instant()));

        namedJdbcTemplate.update(sql, params);
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        var sql = """
                SELECT id, user_id, token_hash, expires_at, created_at
                FROM password_reset_tokens WHERE token_hash = :tokenHash""";

        var params = new MapSqlParameterSource("tokenHash", tokenHash);
        try {
            var token = namedJdbcTemplate.queryForObject(sql, params, rowMapper);
            return Optional.ofNullable(token);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void deleteByUserId(long userId) {
        var sql = "DELETE FROM password_reset_tokens WHERE user_id = :userId";
        var params = new MapSqlParameterSource("userId", userId);
        namedJdbcTemplate.update(sql, params);
    }
}
