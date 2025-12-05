package pl.bpiatek.linkshorteneruserservice.password;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

class PasswordResetTokenRowMapper implements RowMapper<PasswordResetToken> {
    @Override
    public PasswordResetToken mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PasswordResetToken(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("token_hash"),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}
