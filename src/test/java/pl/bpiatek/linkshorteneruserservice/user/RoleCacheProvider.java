package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(RoleCache.class)
public class RoleCacheProvider {
}
