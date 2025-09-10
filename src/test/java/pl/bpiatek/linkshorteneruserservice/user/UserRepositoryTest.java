package pl.bpiatek.linkshorteneruserservice.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Testcontainers
@JdbcTest
@Import(UserRepository.class)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void shouldSaveAndFindUserByEmail() {
        // given
        var newUser = new User(
                null,
                "test@example.com",
                "hashed_password_123",
                List.of("ROLE_USER"),
                false,
                Instant.now()
        );

        // when
        userRepository.save(newUser);

        // then
        var foundUser = userRepository.findByEmail("test@example.com");
        assertThat(foundUser).isPresent();
        assertSoftly(softly -> {
            var user = foundUser.get();
            softly.assertThat(user.email()).isEqualTo(newUser.email());
            softly.assertThat(user.passwordHash()).isEqualTo(newUser.passwordHash());
            softly.assertThat(user.roles()).containsExactly("ROLE_USER");
            softly.assertThat(user.isEmailVerified()).isEqualTo(newUser.isEmailVerified());
            softly.assertThat(user.createdAt()).isNotNull();
        });
    }
}