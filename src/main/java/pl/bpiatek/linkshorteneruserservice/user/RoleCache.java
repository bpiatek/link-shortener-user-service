package pl.bpiatek.linkshorteneruserservice.user;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class RoleCache {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final Map<String, Long> roleCache = new ConcurrentHashMap<>();

    RoleCache(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    @PostConstruct
    void loadRoles() {
        refreshCache();
    }

    @Scheduled(fixedDelayString = "PT5M")
    void refreshCache() {
        String sql = "SELECT id, name FROM roles";
        Map<String, Long> map = namedJdbcTemplate.query(sql, rs -> {
            Map<String, Long> result = new HashMap<>();
            while (rs.next()) {
                result.put(rs.getString("name"), rs.getLong("id"));
            }
            return result;
        });

        if (map != null && !map.isEmpty()) {
            roleCache.clear();
            roleCache.putAll(map);
        }
    }

    Long getRoleId(String roleName) {
        return roleCache.get(roleName);
    }
}