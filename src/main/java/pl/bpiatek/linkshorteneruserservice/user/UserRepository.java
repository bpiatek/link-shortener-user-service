package pl.bpiatek.linkshorteneruserservice.user;

import java.util.Optional;

interface UserRepository {
    Optional<User> findByEmail(String email);
    Optional<User> findById(String id);
    User save(User user);
    void verifyUser(Long userId);
    void updatePassword(Long userId, String newPassword);
}
