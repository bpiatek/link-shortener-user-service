package pl.bpiatek.linkshorteneruserservice.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

class RefreshTokenStore {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenStore.class);

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
        log.info("Deleting refresh token with ID: {}", id);
        repository.deleteById(id);
    }

    @Transactional
    void deleteAllByUserId(Long userId) {
        log.info("Deleting all refresh tokens for user with ID: {}", userId);
        repository.deleteByUserId(userId);
    }
}
