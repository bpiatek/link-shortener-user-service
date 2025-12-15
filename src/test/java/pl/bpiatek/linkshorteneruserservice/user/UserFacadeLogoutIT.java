package pl.bpiatek.linkshorteneruserservice.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class UserFacadeLogoutIT extends IntegrationTest {

    @Autowired
    UserFacade userFacade;

    @Autowired
    UserFixtures userFixtures;

    @Autowired
    RefreshTokenFixtures refreshTokenFixtures;

    @Test
    void shouldLogOutEverywhere() {
        // given
        var userA = userFixtures.aUser();
        refreshTokenFixtures.createActiveToken(userA.getId(), "token-a-1");
        refreshTokenFixtures.createActiveToken(userA.getId(), "token-a-2");

        var userB = userFixtures.aUser(TestUser.builder().email("other@example.com").build());
        refreshTokenFixtures.createActiveToken(userB.getId(), "token-b-1");

        simulateSecurityContext(userA.getId());

        // when
        userFacade.logoutEverywhere();

        // then
        assertThat(refreshTokenFixtures.findAllByUserId(userA.getId())).isEmpty();
        assertThat(refreshTokenFixtures.findAllByUserId(userB.getId())).hasSize(1);
    }

    private void simulateSecurityContext(Long userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId.toString(), null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
