package com.boilerplate.springauth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.models.enums.Role;

public class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp () {

        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret");
        ReflectionTestUtils.setField(tokenService, "expireHours", 2L);

    }

    @Test
    @DisplayName("Should generate a valid token with user email as subject")
    void generateToken_ShouldGenerateValidToken_WithUserEmailAsSubject () {

        User user = createUser();

        String token = tokenService.generateToken(user);
        String subject = tokenService.validateToken(token);

        assertFalse(token.isBlank());
        assertEquals(user.getEmail(), subject);

    }

    @Test
    @DisplayName("Should return empty string when token is invalid")
    void validateToken_ShouldReturnEmptyString_WhenTokenIsInvalid () {

        String subject = tokenService.validateToken("invalid-token");

        assertEquals("", subject);

    }

    @Test
    @DisplayName("Should return empty string when token was signed with another secret")
    void validateToken_ShouldReturnEmptyString_WhenTokenSecretIsDifferent () {

        User user = createUser();
        String token = tokenService.generateToken(user);

        TokenService anotherTokenService = new TokenService();
        ReflectionTestUtils.setField(anotherTokenService, "secret", "another-secret");
        ReflectionTestUtils.setField(anotherTokenService, "expireHours", 2L);

        String subject = anotherTokenService.validateToken(token);

        assertEquals("", subject);

    }

    private User createUser () {

        User user = new User();

        user.setId(1L);
        user.setName("Tiago");
        user.setEmail("tiago@test.com");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        user.setActive(true);

        return user;

    }

}
