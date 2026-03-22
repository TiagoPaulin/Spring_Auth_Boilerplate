package com.boilerplate.springauth.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boilerplate.springauth.dto.login.LoginRequest;
import com.boilerplate.springauth.dto.login.LoginResponse;
import com.boilerplate.springauth.dto.refresh.TokenRefreshRequest;
import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.security.RefreshTokenService;
import com.boilerplate.springauth.security.TokenService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthController (AuthenticationManager authenticationManager, TokenService tokenService, RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login (@Valid @RequestBody LoginRequest request) {

        var usernamePassword = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((User) auth.getPrincipal());
        var refreshToken = refreshTokenService.generateRefreshToken((User) auth.getPrincipal());

        return ResponseEntity.ok(new LoginResponse(token, refreshToken.getToken()));

    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh (@Valid @RequestBody TokenRefreshRequest request) {

        var refreshToken = refreshTokenService.findByToken(request.refreshToken());

        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        String newAccessToken = tokenService.generateToken(user);

        return ResponseEntity.ok(new LoginResponse(newAccessToken, refreshToken.getToken()));

    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout (@RequestBody TokenRefreshRequest request, @AuthenticationPrincipal User user) {

        refreshTokenService.revoke(request.refreshToken(), user);

        return ResponseEntity.noContent().build();

    }

    @DeleteMapping("/logout-all")
    public ResponseEntity<Void> logoutAll (@AuthenticationPrincipal User user) {

        refreshTokenService.revokeAll(user);

        return ResponseEntity.noContent().build();

    }

}
