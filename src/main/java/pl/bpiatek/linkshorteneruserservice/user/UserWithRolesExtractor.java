package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class UserWithRolesExtractor implements ResultSetExtractor<User> {

    @Override
    public User extractData(ResultSet rs) throws SQLException, DataAccessException {
        if (!rs.next()) {
            return null;
        }

        List<String> roles = new ArrayList<>();
        var user = new User(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("password_hash"),
                roles,
                rs.getBoolean("is_email_verified"),
                rs.getTimestamp("created_at").toInstant());


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
