package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.util.Optional;

class JdbcRefreshTokenRepository implements RefreshTokenRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<RefreshToken> ROW_MAPPER = (rs, rowNum) -> new RefreshToken(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("token_hash"),
            rs.getTimestamp("expires_at").toInstant(),
            rs.getTimestamp("created_at").toInstant()
    );

    JdbcRefreshTokenRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(RefreshToken refreshToken) {
        var sql = """
                INSERT INTO refresh_tokens (user_id, token_hash, expires_at, created_at)
                VALUES (:userId, :tokenHash, :expiresAt, :createdAt)
                """;

        var params = new MapSqlParameterSource()
                .addValue("userId", refreshToken.userId())
                .addValue("tokenHash", refreshToken.tokenHash())
                .addValue("expiresAt", Timestamp.from(refreshToken.expiresAt()))
                .addValue("createdAt", Timestamp.from(refreshToken.createdAt()));

        jdbcTemplate.update(sql, params);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        var sql = "SELECT * FROM refresh_tokens WHERE token_hash = :tokenHash";
        var params = new MapSqlParameterSource("tokenHash", tokenHash);

        return jdbcTemplate.query(sql, params, ROW_MAPPER).stream().findFirst();
    }

    @Override
    public void deleteById(Long id) {
        var sql = "DELETE FROM refresh_tokens WHERE id = :id";
        jdbcTemplate.update(sql, new MapSqlParameterSource("id", id));
    }

    @Override
    public void deleteByUserId(Long userId) {
        var sql = "DELETE FROM refresh_tokens WHERE user_id = :userId";
        jdbcTemplate.update(sql, new MapSqlParameterSource("userId", userId));
    }
}
