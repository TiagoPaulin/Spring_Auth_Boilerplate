package com.boilerplate.springauth.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.boilerplate.springauth.models.RefreshToken;
import com.boilerplate.springauth.models.ResetPasswordToken;
import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.models.enums.Role;
import com.boilerplate.springauth.repositories.RefreshTokenRepository;
import com.boilerplate.springauth.repositories.ResetPasswordTokenRepository;
import com.boilerplate.springauth.repositories.UserRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private ResetPasswordTokenRepository resetPasswordTokenRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Should delete user that has refresh and reset password tokens")
    void delete_ShouldRemoveUserAndTokens_WhenUserHasTokens () {

        User user = userRepository.save(createUser());
        refreshTokenRepository.save(new RefreshToken(null, "refresh-token", user, Instant.now().plusSeconds(3600)));
        resetPasswordTokenRepository.save(new ResetPasswordToken(null, "reset-token", user, Instant.now().plusSeconds(600)));

        userService.delete(user.getId());
        // Forces the DELETE statements so foreign key violations surface inside the test
        entityManager.flush();

        assertThat(userRepository.existsById(user.getId())).isFalse();
        assertThat(refreshTokenRepository.findByToken("refresh-token")).isEmpty();
        assertThat(resetPasswordTokenRepository.findByToken("reset-token")).isEmpty();

    }

    private User createUser () {

        User user = new User();

        user.setName("Tiago Teste");
        user.setEmail("delete-me@test.com");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        user.setActive(true);

        return user;

    }

}
