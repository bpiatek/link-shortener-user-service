package pl.bpiatek.linkshorteneruserservice.email;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Component
@ActiveProfiles("test")
class EmailVerificationFixtures {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final SimpleJdbcInsert userInsert;

    EmailVerificationFixtures(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.userInsert = new SimpleJdbcInsert(namedParameterJdbcTemplate.getJdbcTemplate())
                .withTableName("email_verifications")
                .usingGeneratedKeyColumns("id");
    }

    TestEmailVerification anEmailVerification(TestEmailVerification emailVerification) {
        var params = new MapSqlParameterSource()
                .addValue("user_id", emailVerification.getUserId())
                .addValue("token_hash", emailVerification.getTokenHash())
                .addValue("expires_at", Timestamp.from(emailVerification.getExpiresAt()))
                .addValue("created_at", Timestamp.from(emailVerification.getCreatedAt()));

        userInsert.execute(params);

        return getEmailVerificationByTokenHash(emailVerification.getTokenHash());
    }

    TestEmailVerification getEmailVerificationByTokenHash(String tokenHash) {
        var sql = """
            SELECT id, user_id, token_hash, expires_at, created_at
            FROM email_verifications WHERE token_hash = :tokenHash""";

        var params = new MapSqlParameterSource().addValue("tokenHash", tokenHash);

        try {
            return namedParameterJdbcTemplate.query(sql, params, new TestEmailVerificationExtractor());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    Integer countForUser(Long userId) {
        var sql = "SELECT COUNT(*) FROM email_verifications WHERE user_id = :userId";
        var params = new MapSqlParameterSource().addValue("userId", userId);
        return namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
    }

    static class TestEmailVerificationExtractor implements ResultSetExtractor<TestEmailVerification> {

        @Override
        public TestEmailVerification extractData(ResultSet rs) throws SQLException, DataAccessException {
            if (!rs.next()) {
                return null;
            }

            return TestEmailVerification.builder()
                    .id(rs.getLong("id"))
                    .userId(rs.getLong("user_id"))
                    .tokenHash(rs.getString("token_hash"))
                    .expiresAt(rs.getTimestamp("expires_at").toInstant())
                    .createdAt(rs.getTimestamp("created_at").toInstant())
                    .build();
        }
    }
}
