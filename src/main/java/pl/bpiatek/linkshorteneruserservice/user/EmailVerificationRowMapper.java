package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

class EmailVerificationRowMapper implements RowMapper<EmailVerification> {

    @Override
    public EmailVerification mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new EmailVerification(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("token_hash"),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}