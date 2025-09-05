package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<User> findByEmail(String email) {
        var sql = "SELECT id, email, password_hash, role, is_email_verified, created_at FROM users WHERE email = ?";
        try {
            var user = jdbcTemplate.queryForObject(sql, userRowMapper, email);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    void save(User user) {
        var sql = "INSERT INTO users (email, password_hash, role, is_email_verified, created_at) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                user.email(),
                user.passwordHash(),
                user.role(),
                user.isEmailVerified(),
                java.sql.Timestamp.from(user.createdAt())
        );
    }

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("role"),
            rs.getBoolean("is_email_verified"),
            rs.getTimestamp("created_at").toInstant()
    );
}
