package com.boilerplate.springauth.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.boilerplate.springauth.models.RefreshToken;
import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.models.enums.Role;

@DataJpaTest
@ActiveProfiles("test")
public class RefreshTokenRepositoryTest {
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should delete only tokens with expiry date in the past")
    void deleteByExpiryDateBefore_ShouldDeleteOnlyExpiredTokens () {

        User user = createAndPersistUser();
        Instant now = Instant.now();

        createAndPersistToken("expired-token", user, now.minusSeconds(3600));
        createAndPersistToken("valid-token", user, now.plusSeconds(3600));

        refreshTokenRepository.deleteByExpiryDateBefore(now);

        entityManager.flush();
        entityManager.clear();

        List<RefreshToken> remainingTokens = refreshTokenRepository.findAll();

        assertThat(remainingTokens).hasSize(1);
        assertThat(remainingTokens.get(0).getToken()).isEqualTo("valid-token");

    }

    private User createAndPersistUser () {

        User user = new User();
        
        user.setName("Tiago Teste");
        user.setEmail("tiago@test.com");
        user.setPassword("password123");
        user.setRole(Role.USER);

        return entityManager.persist(user);

    }

    private void createAndPersistToken (String tokenValue, User user, Instant expiry) {

        RefreshToken token = new RefreshToken();

        token.setToken(tokenValue);
        token.setUser(user);
        token.setExpiryDate(expiry);

        entityManager.persist(token);

    }

}
