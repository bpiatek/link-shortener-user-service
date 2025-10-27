package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ActiveProfiles("test")
class UserFixtures {

    @Autowired
    private RoleCache roleCache;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final SimpleJdbcInsert userInsert;

    UserFixtures(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.userInsert = new SimpleJdbcInsert(namedParameterJdbcTemplate.getJdbcTemplate())
                .withTableName("users")
                .usingGeneratedKeyColumns("id");
    }

    TestUser aUser(TestUser user) {
        var params = new MapSqlParameterSource()
                .addValue("email", user.getEmail())
                .addValue("password_hash", user.getPasswordHash())
                .addValue("is_email_verified", user.isEmailVerified())
                .addValue("created_at", Timestamp.from(user.getCreatedAt()));

        var generatedId = userInsert.executeAndReturnKey(params);
        var userId = generatedId.longValue();

        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            insertUserRoles(userId, user.getRoles());
        }

        return getUserByEmail(user.getEmail());
    }

    TestUser getUserByEmail(String email) {
        var sql = """
            SELECT u.id, u.email, u.password_hash, u.is_email_verified, u.created_at, r.name as role_name
            FROM users u
            LEFT JOIN user_roles ur ON u.id = ur.user_id
            LEFT JOIN roles r ON ur.role_id = r.id
            WHERE u.email = :email
            """;
        try {
            return namedParameterJdbcTemplate.query(sql, Map.of("email", email), new UserWithRolesExtractor());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private void insertUserRoles(Long userId, List<String> roles) {
        var insertSql = "INSERT INTO user_roles (user_id, role_id) VALUES (:userId, :roleId)";

        var batchParams = roles.stream()
                .map(role -> {
                    var roleId = roleCache.getRoleId(role);
                    if (roleId == null) {
                        throw new IllegalArgumentException("Unknown role: " + role);
                    }

                    return new MapSqlParameterSource()
                            .addValue("userId", userId)
                            .addValue("roleId", roleId);
                })
                .toArray(MapSqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(insertSql, batchParams);
    }

    static class UserWithRolesExtractor implements ResultSetExtractor<TestUser> {

        @Override
        public TestUser extractData(ResultSet rs) throws SQLException, DataAccessException {
            if (!rs.next()) {
                return null;
            }

            List<String> roles = new ArrayList<>();
            var user = TestUser.builder()
                    .id(rs.getLong("id"))
                    .email(rs.getString("email"))
                    .passwordHash(rs.getString("password_hash"))
                    .isEmailVerified(rs.getBoolean("is_email_verified"))
                    .createdAt(rs.getTimestamp("created_at").toInstant())
                    .roles(roles)
                    .build();

            var roleName = rs.getString("role_name");
            if (roleName != null) {
                roles.add(roleName);
            }

            while (rs.next()) {
                roleName = rs.getString("role_name");
                if (roleName != null) {
                    roles.add(roleName);
                }
            }

            return user;
        }
    }
}
