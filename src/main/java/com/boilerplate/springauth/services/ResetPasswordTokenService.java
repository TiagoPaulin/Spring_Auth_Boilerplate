package com.boilerplate.springauth.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.boilerplate.springauth.dto.password.ForgotPasswordRequest;
import com.boilerplate.springauth.dto.password.ResetPasswordRequest;
import com.boilerplate.springauth.exceptions.BadRequestException;
import com.boilerplate.springauth.exceptions.ResourceNotFoundException;
import com.boilerplate.springauth.models.ResetPasswordToken;
import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.repositories.RefreshTokenRepository;
import com.boilerplate.springauth.repositories.ResetPasswordTokenRepository;
import com.boilerplate.springauth.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class ResetPasswordTokenService {

    private final ResetPasswordTokenRepository repository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    @Value("${api.security.reset-password.expiration}")
    private Long expireMinutes;

    public ResetPasswordTokenService (ResetPasswordTokenRepository repository, UserRepository userRepository, PasswordEncoder passwordEncoder, RefreshTokenRepository refreshTokenRepository, MailService mailService) {

        this.repository = repository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.mailService = mailService;

    }

    public void requestPasswordReset (ForgotPasswordRequest request) {

        userRepository.findOptionalByEmail(request.email()).ifPresent(user -> {

            ResetPasswordToken resetPasswordToken = new ResetPasswordToken();

            resetPasswordToken.setUser(user);
            resetPasswordToken.setToken(UUID.randomUUID().toString());
            resetPasswordToken.setExpiryDate(Instant.now().plus(expireMinutes, ChronoUnit.MINUTES));

            repository.save(resetPasswordToken);
            mailService.sendPasswordResetEmail(user.getEmail(), resetPasswordToken.getToken(), expireMinutes);

        });

    }

    @Transactional
    public void changeUserPassword (ResetPasswordRequest request) {

        ResetPasswordToken resetPasswordToken = findToken(request.resetToken());

        if (resetPasswordToken.isExpired()) {
            resetPasswordToken.setUsed(true);
            repository.save(resetPasswordToken);
            throw new BadRequestException("Reset Password token expired");
        }

        User user = resetPasswordToken.getUser();

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        resetPasswordToken.setUsed(true);
        repository.save(resetPasswordToken);

        refreshTokenRepository.deleteByUser(user);

    }

    private ResetPasswordToken findToken (String token) {

        return repository.findByToken(token).orElseThrow(() -> new ResourceNotFoundException("token not found"));

    }

}
