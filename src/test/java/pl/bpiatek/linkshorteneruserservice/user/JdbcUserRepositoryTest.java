package pl.bpiatek.linkshorteneruserservice.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.bpiatek.linkshorteneruserservice.WithPostgres;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static pl.bpiatek.linkshorteneruserservice.user.TestUser.builder;

@Testcontainers
@JdbcTest
@Import({JdbcUserRepository.class, RoleCache.class, UserFixtures.class})
@ActiveProfiles("test")
class JdbcUserRepositoryTest implements WithPostgres {

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    UserFixtures userFixtures;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void shouldSaveUser() {
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
        var foundUser = userFixtures.getUserByEmail("test@example.com");
        assertThat(foundUser).isNotNull();
        assertSoftly(softly -> {
            softly.assertThat(foundUser.getEmail()).isEqualTo(newUser.email());
            softly.assertThat(foundUser.getPasswordHash()).isEqualTo(newUser.passwordHash());
            softly.assertThat(foundUser.getRoles()).containsExactly("ROLE_USER");
            softly.assertThat(foundUser.isEmailVerified()).isEqualTo(newUser.isEmailVerified());
            softly.assertThat(foundUser.getCreatedAt()).isNotNull();
        });
    }

    @Test
    void shouldFindUserByEmail() {
        // given
        var userToFind = userFixtures.aUser(builder().email("usertofind@test.com").build());

        // when
        var foundUser = userRepository.findByEmail(userToFind.getEmail());

        // then
        assertThat(foundUser).isPresent();
        assertSoftly(softly -> {
            var user = foundUser.get();
            softly.assertThat(user.id()).isEqualTo(userToFind.getId());
            softly.assertThat(user.email()).isEqualTo(userToFind.getEmail());
            softly.assertThat(user.passwordHash()).isEqualTo(userToFind.getPasswordHash());
            softly.assertThat(user.roles()).containsExactly("ROLE_USER");
            softly.assertThat(user.isEmailVerified()).isEqualTo(userToFind.isEmailVerified());
            softly.assertThat(user.createdAt()).isNotNull();
        });
    }

    @Test
    void userShouldBeAbleToHaveMultipleRoles() {
        // given
        var user = userFixtures.aUser(builder()
                .roles(List.of("ROLE_ADMIN", "ROLE_USER"))
                .build());

        // when
        var foundUser = userRepository.findByEmail(user.getEmail());

        // then
        assertThat(foundUser).isPresent();
        assertSoftly(softly -> {
            var userFromDb = foundUser.get();
            softly.assertThat(userFromDb.roles()).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        });
    }
}