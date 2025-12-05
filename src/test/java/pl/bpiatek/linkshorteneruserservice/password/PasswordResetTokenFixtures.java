package pl.bpiatek.linkshorteneruserservice.password;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import pl.bpiatek.linkshorteneruserservice.user.UserFixtures;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

@Component
@ActiveProfiles("test")
public class PasswordResetTokenFixtures {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final SimpleJdbcInsert tokenInsert;

    PasswordResetTokenFixtures(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.tokenInsert = new SimpleJdbcInsert(namedParameterJdbcTemplate.getJdbcTemplate())
                .withTableName("password_reset_tokens")
                .usingGeneratedKeyColumns("id");
    }

    public TestPasswordResetToken aPasswordResetToken() {
        return aPasswordResetToken(TestPasswordResetToken.builder().build());
    }

    public TestPasswordResetToken aPasswordResetToken(TestPasswordResetToken token) {
        var params = new MapSqlParameterSource()
                .addValue("user_id", token.getUserId())
                .addValue("token_hash", token.getTokenHash())
                .addValue("expires_at", Timestamp.from(token.getExpiresAt()))
                .addValue("created_at", Timestamp.from(token.getCreatedAt()));

        tokenInsert.execute(params);

        return getTokenByUserId(token.getUserId());
    }

    public TestPasswordResetToken getTokenByUserId(Long userId) {
        var sql = """
                SELECT t.id, t.user_id, t.token_hash, t.expires_at, t.created_at
                FROM password_reset_tokens t
                WHERE t.user_id = :userId""";

        try {
            return namedParameterJdbcTemplate.query(sql, Map.of("userId", userId),
                    new PasswordResetTokenFixtures.PasswordResetTokenExtractor());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    static class PasswordResetTokenExtractor implements ResultSetExtractor<TestPasswordResetToken> {

        @Override
        public TestPasswordResetToken extractData(ResultSet rs) throws SQLException, DataAccessException {
            if (!rs.next()) {
                return null;
            }

            return TestPasswordResetToken.builder()
                    .id(rs.getLong("id"))
                    .userId(rs.getLong("user_id"))
                    .tokenHash(rs.getString("token_hash"))
                    .expiresAt(rs.getTimestamp("expires_at").toInstant())
                    .createdAt(rs.getTimestamp("created_at").toInstant())
                    .build();
        }
    }
}
