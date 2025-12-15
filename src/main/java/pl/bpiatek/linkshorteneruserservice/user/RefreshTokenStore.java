package pl.bpiatek.linkshorteneruserservice.user;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

class RefreshTokenStore {

    private final RefreshTokenRepository repository;

    RefreshTokenStore(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash);
    }

    void save(RefreshToken refreshToken) {
        repository.save(refreshToken);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    void deleteAllByUserId(Long userId) {
        repository.deleteByUserId(userId);
    }
}
