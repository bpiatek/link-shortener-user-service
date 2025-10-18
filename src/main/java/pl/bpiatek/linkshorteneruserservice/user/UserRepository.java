package pl.bpiatek.linkshorteneruserservice.user;

import java.util.Optional;

interface UserRepository {
    Optional<User> findByEmail(String email);
    User save(User user);
}
