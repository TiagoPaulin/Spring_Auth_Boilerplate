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
import com.boilerplate.springauth.dto.password.ForgotPasswordRequest;
import com.boilerplate.springauth.dto.password.ForgotPasswordResponse;
import com.boilerplate.springauth.dto.password.ResetPasswordRequest;
import com.boilerplate.springauth.dto.refresh.TokenRefreshRequest;
import com.boilerplate.springauth.models.ResetPasswordToken;
import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.security.RefreshTokenService;
import com.boilerplate.springauth.security.TokenService;
import com.boilerplate.springauth.services.ResetPasswordTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication and session management")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final ResetPasswordTokenService resetPasswordTokenService;

    public AuthController (AuthenticationManager authenticationManager, TokenService tokenService, RefreshTokenService refreshTokenService, ResetPasswordTokenService resetPasswordTokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.resetPasswordTokenService = resetPasswordTokenService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates credentials and returns a short-lived Access Token (JWT) and a long-lived Refresh Token.")
    public ResponseEntity<LoginResponse> login (@Valid @RequestBody LoginRequest request) {

        var usernamePassword = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((User) auth.getPrincipal());
        var refreshToken = refreshTokenService.generateRefreshToken((User) auth.getPrincipal());

        return ResponseEntity.ok(new LoginResponse(token, refreshToken.getToken()));

    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Uses a valid Refresh Token to generate a new Access Token without requiring the user to log in again.")
    public ResponseEntity<LoginResponse> refresh (@Valid @RequestBody TokenRefreshRequest request) {

        var refreshToken = refreshTokenService.findByToken(request.refreshToken());

        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        String newAccessToken = tokenService.generateToken(user);

        return ResponseEntity.ok(new LoginResponse(newAccessToken, refreshToken.getToken()));

    }

    @PostMapping("/logout")
    @Operation(summary = "Logout specific session", description = "Revokes a specific Refresh Token, effectively logging out the user from that specific device/session.")
    public ResponseEntity<Void> logout (@RequestBody TokenRefreshRequest request, @AuthenticationPrincipal User user) {

        refreshTokenService.revoke(request.refreshToken(), user);

        return ResponseEntity.noContent().build();

    }

    @DeleteMapping("/logout-all")
    @Operation(summary = "Logout from all devices", description = "Revokes all Refresh Tokens associated with the authenticated user, forcing a logout on all devices.")
    public ResponseEntity<Void> logoutAll (@AuthenticationPrincipal User user) {

        refreshTokenService.revokeAll(user);

        return ResponseEntity.noContent().build();

    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Generate a reset password token", description = "")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword (@Valid @RequestBody ForgotPasswordRequest request) {

        ResetPasswordToken resetPasswordToken = resetPasswordTokenService.generateResetPasswordToken(request);

        return ResponseEntity.ok(new ForgotPasswordResponse(resetPasswordToken.getToken()));

    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset user's password", description = "Reset user's password with a valid token")
    public ResponseEntity<Void> resetPassword (@Valid @RequestBody ResetPasswordRequest request) {

        resetPasswordTokenService.changeUserPassword(request);

        return ResponseEntity.noContent().build();

    }

}
