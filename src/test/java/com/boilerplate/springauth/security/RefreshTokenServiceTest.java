package com.boilerplate.springauth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.boilerplate.springauth.exceptions.BadRequestException;
import com.boilerplate.springauth.exceptions.ForbiddenException;
import com.boilerplate.springauth.exceptions.ResourceNotFoundException;
import com.boilerplate.springauth.models.RefreshToken;
import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.models.enums.Role;
import com.boilerplate.springauth.repositories.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp () {

        ReflectionTestUtils.setField(refreshTokenService, "expireDays", 7L);

    }

    @Test
    @DisplayName("Should generate refresh token for user")
    void generateRefreshToken_ShouldGenerateRefreshToken_ForUser () {

        User user = createUser(1L);

        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken refreshToken = invocation.getArgument(0);
            refreshToken.setId(1L);
            return refreshToken;
        });

        RefreshToken response = refreshTokenService.generateRefreshToken(user);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(user, savedToken.getUser());
        assertNotNull(savedToken.getToken());
        assertFalse(savedToken.getToken().isBlank());
        assertNotNull(savedToken.getExpiryDate());
        assertFalse(savedToken.isExpired());

    }

    @Test
    @DisplayName("Should find refresh token when token exists")
    void findByToken_ShouldReturnRefreshToken_WhenTokenExists () {

        RefreshToken refreshToken = createRefreshToken("refresh-token", createUser(1L), Instant.now().plusSeconds(3600));

        when(repository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));

        RefreshToken response = refreshTokenService.findByToken("refresh-token");

        assertSame(refreshToken, response);
        verify(repository, times(1)).findByToken("refresh-token");

    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when refresh token does not exist")
    void findByToken_ShouldThrowException_WhenTokenDoesNotExist () {

        when(repository.findByToken("missing-token")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            refreshTokenService.findByToken("missing-token");
        });

    }

    @Test
    @DisplayName("Should delete expired tokens")
    void deleteExpiredTokens_ShouldDeleteExpiredTokens () {

        refreshTokenService.deleteExpiredTokens();

        verify(repository, times(1)).deleteByExpiryDateBefore(any(Instant.class));

    }

    @Test
    @DisplayName("Should return token when refresh token is not expired")
    void verifyExpiration_ShouldReturnToken_WhenTokenIsNotExpired () {

        RefreshToken refreshToken = createRefreshToken("refresh-token", createUser(1L), Instant.now().plusSeconds(3600));

        RefreshToken response = refreshTokenService.verifyExpiration(refreshToken);

        assertSame(refreshToken, response);
        verify(repository, never()).delete(any(RefreshToken.class));

    }

    @Test
    @DisplayName("Should delete token and throw BadRequestException when refresh token is expired")
    void verifyExpiration_ShouldDeleteTokenAndThrowException_WhenTokenIsExpired () {

        RefreshToken refreshToken = createRefreshToken("refresh-token", createUser(1L), Instant.now().minusSeconds(3600));

        assertThrows(BadRequestException.class, () -> {
            refreshTokenService.verifyExpiration(refreshToken);
        });

        verify(repository, times(1)).delete(refreshToken);

    }

    @Test
    @DisplayName("Should revoke refresh token when token belongs to user")
    void revoke_ShouldDeleteRefreshToken_WhenTokenBelongsToUser () {

        User user = createUser(1L);
        RefreshToken refreshToken = createRefreshToken("refresh-token", user, Instant.now().plusSeconds(3600));

        when(repository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));

        refreshTokenService.revoke("refresh-token", user);

        verify(repository, times(1)).delete(refreshToken);

    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when revoking token that does not exist")
    void revoke_ShouldThrowException_WhenTokenDoesNotExist () {

        User user = createUser(1L);

        when(repository.findByToken("missing-token")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            refreshTokenService.revoke("missing-token", user);
        });

        verify(repository, never()).delete(any(RefreshToken.class));

    }

    @Test
    @DisplayName("Should throw ForbiddenException when token does not belong to user")
    void revoke_ShouldThrowException_WhenTokenDoesNotBelongToUser () {

        User tokenOwner = createUser(1L);
        User anotherUser = createUser(2L);
        RefreshToken refreshToken = createRefreshToken("refresh-token", tokenOwner, Instant.now().plusSeconds(3600));

        when(repository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));

        assertThrows(ForbiddenException.class, () -> {
            refreshTokenService.revoke("refresh-token", anotherUser);
        });

        verify(repository, never()).delete(any(RefreshToken.class));

    }

    @Test
    @DisplayName("Should revoke all refresh tokens from user")
    void revokeAll_ShouldDeleteRefreshTokensFromUser () {

        User user = createUser(1L);

        refreshTokenService.revokeAll(user);

        verify(repository, times(1)).deleteByUser(user);

    }

    private User createUser (Long id) {

        User user = new User();

        user.setId(id);
        user.setName("Tiago");
        user.setEmail("tiago" + id + "@test.com");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        user.setActive(true);

        return user;

    }

    private RefreshToken createRefreshToken (String token, User user, Instant expiryDate) {

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setId(1L);
        refreshToken.setToken(token);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(expiryDate);

        return refreshToken;

    }

}
