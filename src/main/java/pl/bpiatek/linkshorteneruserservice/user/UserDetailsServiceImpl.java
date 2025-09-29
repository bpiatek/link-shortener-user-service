package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


import java.util.stream.Collectors;

@Service
class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(dbUser -> {
                    var authorities = dbUser.roles().stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    return new User(
                            dbUser.email(),
                            dbUser.passwordHash(),
                            authorities
                    );
                })
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}
