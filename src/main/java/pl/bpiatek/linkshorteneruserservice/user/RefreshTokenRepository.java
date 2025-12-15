package pl.bpiatek.linkshorteneruserservice.user;

import java.util.Optional;

interface  RefreshTokenRepository {
    void save(RefreshToken refreshToken);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void deleteById(Long id);
    void deleteByUserId(Long userId);
}
