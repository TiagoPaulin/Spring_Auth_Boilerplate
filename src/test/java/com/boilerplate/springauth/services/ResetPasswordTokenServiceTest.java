package com.boilerplate.springauth.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.boilerplate.springauth.dto.password.ForgotPasswordRequest;
import com.boilerplate.springauth.dto.password.ResetPasswordRequest;
import com.boilerplate.springauth.exceptions.BadRequestException;
import com.boilerplate.springauth.exceptions.ResourceNotFoundException;
import com.boilerplate.springauth.models.ResetPasswordToken;
import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.models.enums.Role;
import com.boilerplate.springauth.repositories.RefreshTokenRepository;
import com.boilerplate.springauth.repositories.ResetPasswordTokenRepository;
import com.boilerplate.springauth.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
public class ResetPasswordTokenServiceTest {

    @Mock
    private ResetPasswordTokenRepository repository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private MailService mailService;

    @InjectMocks
    private ResetPasswordTokenService resetPasswordTokenService;

    @BeforeEach
    void setUp () {

        ReflectionTestUtils.setField(resetPasswordTokenService, "expireMinutes", 15L);

    }

    @Test
    @DisplayName("Should save reset password token and email it when user exists")
    void requestPasswordReset_ShouldSaveTokenAndSendEmail_WhenUserExists () {

        ForgotPasswordRequest request = new ForgotPasswordRequest("tiago@test.com");
        User user = createUser(1L);

        when(userRepository.findOptionalByEmail(request.email())).thenReturn(Optional.of(user));

        resetPasswordTokenService.requestPasswordReset(request);

        ArgumentCaptor<ResetPasswordToken> tokenCaptor = ArgumentCaptor.forClass(ResetPasswordToken.class);
        verify(repository).save(tokenCaptor.capture());

        ResetPasswordToken savedToken = tokenCaptor.getValue();
        assertEquals(user, savedToken.getUser());
        assertNotNull(savedToken.getToken());
        assertFalse(savedToken.getToken().isBlank());
        assertNotNull(savedToken.getExpiryDate());
        assertFalse(savedToken.getUsed());
        assertFalse(savedToken.isExpired());
        verify(mailService).sendPasswordResetEmail(user.getEmail(), savedToken.getToken(), 15L);

    }

    @Test
    @DisplayName("Should neither save token nor send email when user does not exist")
    void requestPasswordReset_ShouldDoNothing_WhenUserDoesNotExist () {

        ForgotPasswordRequest request = new ForgotPasswordRequest("missing@test.com");

        when(userRepository.findOptionalByEmail(request.email())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> resetPasswordTokenService.requestPasswordReset(request));

        verify(repository, never()).save(any(ResetPasswordToken.class));
        verifyNoInteractions(mailService);

    }

    @Test
    @DisplayName("Should change user password and revoke refresh tokens when reset token is valid")
    void changeUserPassword_ShouldChangePasswordAndRevokeRefreshTokens_WhenTokenIsValid () {

        User user = createUser(1L);
        ResetPasswordToken resetPasswordToken = createResetPasswordToken("reset-token", user, Instant.now().plusSeconds(3600), false);
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "newPassword123");

        when(repository.findByToken(request.resetToken())).thenReturn(Optional.of(resetPasswordToken));
        when(passwordEncoder.encode(request.newPassword())).thenReturn("new-encoded-password");

        resetPasswordTokenService.changeUserPassword(request);

        assertEquals("new-encoded-password", user.getPassword());
        assertTrue(resetPasswordToken.getUsed());
        verify(userRepository, times(1)).save(user);
        verify(repository, times(1)).save(resetPasswordToken);
        verify(refreshTokenRepository, times(1)).deleteByUser(user);

    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when reset token does not exist")
    void changeUserPassword_ShouldThrowException_WhenTokenDoesNotExist () {

        ResetPasswordRequest request = new ResetPasswordRequest("missing-token", "newPassword123");

        when(repository.findByToken(request.resetToken())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            resetPasswordTokenService.changeUserPassword(request);
        });

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
        verify(refreshTokenRepository, never()).deleteByUser(any(User.class));

    }

    @Test
    @DisplayName("Should mark token as used and throw BadRequestException when reset token is expired")
    void changeUserPassword_ShouldMarkTokenAsUsedAndThrowException_WhenTokenIsExpired () {

        User user = createUser(1L);
        ResetPasswordToken resetPasswordToken = createResetPasswordToken("expired-token", user, Instant.now().minusSeconds(3600), false);
        ResetPasswordRequest request = new ResetPasswordRequest("expired-token", "newPassword123");

        when(repository.findByToken(request.resetToken())).thenReturn(Optional.of(resetPasswordToken));

        assertThrows(BadRequestException.class, () -> {
            resetPasswordTokenService.changeUserPassword(request);
        });

        assertTrue(resetPasswordToken.getUsed());
        assertEquals("encoded-password", user.getPassword());
        verify(repository, times(1)).save(resetPasswordToken);
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
        verify(refreshTokenRepository, never()).deleteByUser(any(User.class));

    }

    @Test
    @DisplayName("Should throw BadRequestException when reset token was already used")
    void changeUserPassword_ShouldThrowException_WhenTokenWasAlreadyUsed () {

        User user = createUser(1L);
        ResetPasswordToken resetPasswordToken = createResetPasswordToken("used-token", user, Instant.now().plusSeconds(3600), true);
        ResetPasswordRequest request = new ResetPasswordRequest("used-token", "newPassword123");

        when(repository.findByToken(request.resetToken())).thenReturn(Optional.of(resetPasswordToken));

        assertThrows(BadRequestException.class, () -> {
            resetPasswordTokenService.changeUserPassword(request);
        });

        assertTrue(resetPasswordToken.getUsed());
        assertEquals("encoded-password", user.getPassword());
        verify(repository, times(1)).save(resetPasswordToken);
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
        verify(refreshTokenRepository, never()).deleteByUser(any(User.class));

    }

    private User createUser (Long id) {

        User user = new User();

        user.setId(id);
        user.setName("Tiago");
        user.setEmail("tiago@test.com");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        user.setActive(true);

        return user;

    }

    private ResetPasswordToken createResetPasswordToken (String token, User user, Instant expiryDate, boolean used) {

        ResetPasswordToken resetPasswordToken = new ResetPasswordToken();

        resetPasswordToken.setId(1L);
        resetPasswordToken.setToken(token);
        resetPasswordToken.setUser(user);
        resetPasswordToken.setExpiryDate(expiryDate);
        resetPasswordToken.setUsed(used);

        return resetPasswordToken;

    }

}
