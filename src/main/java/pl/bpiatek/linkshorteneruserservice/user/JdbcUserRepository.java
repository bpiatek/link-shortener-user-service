package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class JdbcUserRepository implements  UserRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final SimpleJdbcInsert userInsert;
    private final RoleCache roleCache;

    JdbcUserRepository(NamedParameterJdbcTemplate namedJdbcTemplate, RoleCache roleCache) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.userInsert = new SimpleJdbcInsert(namedJdbcTemplate.getJdbcTemplate())
                .withTableName("users")
                .usingGeneratedKeyColumns("id");
        this.roleCache = roleCache;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        var sql = """
            SELECT u.id, u.email, u.password_hash, u.is_email_verified, u.created_at, r.name as role_name
            FROM users u
            LEFT JOIN user_roles ur ON u.id = ur.user_id
            LEFT JOIN roles r ON ur.role_id = r.id
            WHERE u.email = :email
            """;

        try {
            var user = namedJdbcTemplate.query(sql, Map.of("email", email), new UserWithRolesExtractor());
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public User save(User user) {
        Map<String, Object> params = Map.of(
                "email", user.email(),
                "password_hash", user.passwordHash(),
                "is_email_verified", user.isEmailVerified(),
                "created_at", Timestamp.from(user.createdAt())
        );

        var key = userInsert.executeAndReturnKey(params);
        long generatedId = key.longValue();

        if (user.roles() != null && !user.roles().isEmpty()) {
            insertUserRoles(generatedId, user.roles());
        }

        return new User(
                generatedId,
                user.email(),
                user.passwordHash(),
                user.roles(),
                user.isEmailVerified(),
                user.createdAt());
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

        namedJdbcTemplate.batchUpdate(insertSql, batchParams);
    }
}
