package com.boilerplate.springauth.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.boilerplate.springauth.exceptions.ResourceNotFoundException;
import com.boilerplate.springauth.models.RefreshToken;
import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.repositories.RefreshTokenRepository;
import com.boilerplate.springauth.exceptions.BadRequestException;
import com.boilerplate.springauth.exceptions.ForbiddenException;

import jakarta.transaction.Transactional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    @Value("${api.security.refresh.expiration}")
    private Long expireDays;

    public RefreshTokenService (RefreshTokenRepository repository) {

        this.repository = repository;

    }

    public RefreshToken generateRefreshToken (User user) {

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plus(expireDays, ChronoUnit.DAYS));

        return repository.save(refreshToken);

    }

    public RefreshToken findByToken (String token) {

        return repository.findByToken(token).orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

    }

    @Transactional
    public RefreshToken verifyExpiration (RefreshToken token) {

        if (token.isExpired()) {
            repository.delete(token);
            throw new BadRequestException("Refresh token expired");
        }
        return token;

    }

    @Transactional
    public void revoke (String token, User user) {
        
        RefreshToken refreshToken = repository.findByToken(token).orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

        if (!refreshToken.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Token does not belong to user");
        }

        repository.delete(refreshToken);
        
    }

    @Transactional
    public void revokeAll (User user) {

        repository.deleteByUser(user);
        
    }
    
}
