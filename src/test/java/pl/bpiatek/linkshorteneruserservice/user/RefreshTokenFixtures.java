package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static pl.bpiatek.linkshorteneruserservice.user.IntegrationTest.DEFAULT_NOW;

@Component
@ActiveProfiles("test")
class RefreshTokenFixtures {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final SimpleJdbcInsert tokenInsert;

    RefreshTokenFixtures(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.tokenInsert = new SimpleJdbcInsert(namedParameterJdbcTemplate.getJdbcTemplate())
                .withTableName("refresh_tokens")
                .usingGeneratedKeyColumns("id");
    }

    TestRefreshToken aRefreshToken(TestRefreshToken token) {
        var params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("user_id", token.getUserId())
                .addValue("token_hash", token.getTokenHash())
                .addValue("expires_at", java.sql.Timestamp.from(token.getExpiresAt()))
                .addValue("created_at", java.sql.Timestamp.from(token.getCreatedAt()));

       tokenInsert.execute(params);

        return findAllByUserId(token.getUserId()).getFirst();
    }

    TestRefreshToken aRefreshToken(Long userId) {
        return aRefreshToken(TestRefreshToken.builder(userId).build());
    }

    TestRefreshToken getRefreshTokenForUser(Long userId) {
        var tokens = findAllByUserId(userId);
        if (tokens.size() != 1) {
            throw new AssertionError("Expected exactly one token for user " + userId + " but found " + tokens.size());
        }
        return tokens.getFirst();
    }

    List<TestRefreshToken> findAllByUserId(Long userId) {
        var sql = """
                SELECT t.id, t.user_id, t.token_hash, t.expires_at,
                t.created_at
                FROM refresh_tokens t
                WHERE t.user_id = :userId""";

            var params = new MapSqlParameterSource("userId", userId);
            return namedParameterJdbcTemplate.query(sql, params,
                    new RefreshTokenFixtures.RefreshTokenExtractor());
    }

    TestRefreshToken getRefreshTokenById(Long id) {
        var sql = """
                SELECT t.id, t.user_id, t.token_hash, t.expires_at,
                t.created_at
                FROM refresh_tokens t
                WHERE t.id = :id""";

        var params = new MapSqlParameterSource("id", id);
        try {
            return namedParameterJdbcTemplate.queryForObject(sql, params, new RefreshTokenFixtures.RefreshTokenExtractor());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    void createActiveToken(Long userId, String rawToken) {
        aRefreshToken(
                TestRefreshToken.builder(userId)
                        .hashTokenFromRaw(rawToken)
                        .createdAt(Instant.now())
                        .expiresAt(Instant.now().plus(7, DAYS))
                        .build()
        );
    }

    void createExpiredToken(Long userId, String rawToken) {
        aRefreshToken(
                TestRefreshToken.builder(userId)
                        .hashTokenFromRaw(rawToken)
                        .createdAt(DEFAULT_NOW.minus(2, DAYS))
                        .expiresAt(DEFAULT_NOW.minus(1, MINUTES))
                        .build()
        );
    }

    static class RefreshTokenExtractor implements RowMapper<TestRefreshToken> {


        @Override
        public TestRefreshToken mapRow(ResultSet rs, int rowNum) throws SQLException {
            return TestRefreshToken.builder(rs.getLong("user_id"))
                    .id(rs.getLong("id"))
                    .tokenHash(rs.getString("token_hash"))
                    .createdAt(rs.getTimestamp("created_at").toInstant())
                    .expiresAt(rs.getTimestamp("expires_at").toInstant())
                    .build();
        }
    }

}
