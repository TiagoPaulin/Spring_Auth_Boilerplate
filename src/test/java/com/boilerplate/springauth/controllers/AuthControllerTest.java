package com.boilerplate.springauth.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.boilerplate.springauth.dto.login.LoginRequest;
import com.boilerplate.springauth.dto.refresh.TokenRefreshRequest;
import com.boilerplate.springauth.exceptions.BadRequestException;
import com.boilerplate.springauth.exceptions.ResourceNotFoundException;
import com.boilerplate.springauth.models.RefreshToken;
import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.models.enums.Role;
import com.boilerplate.springauth.repositories.UserRepository;
import com.boilerplate.springauth.security.RefreshTokenService;
import com.boilerplate.springauth.security.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private TokenService tokenService;
    @MockitoBean
    private RefreshTokenService refreshTokenService;
    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("Should login and return access token and refresh token")
    void login_ShouldReturnAccessTokenAndRefreshToken_WhenCredentialsAreValid () throws Exception {

        LoginRequest request = new LoginRequest("tiago@test.com", "password123");
        User user = createUser(1L, "Tiago", "tiago@test.com", Role.USER);
        RefreshToken refreshToken = createRefreshToken("refresh-token", user, Instant.now().plusSeconds(3600));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(tokenService.generateToken(user)).thenReturn("access-token");
        when(refreshTokenService.generateRefreshToken(user)).thenReturn(refreshToken);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService).generateToken(user);
        verify(refreshTokenService).generateRefreshToken(user);

    }

    @Test
    @DisplayName("Should return bad request when login body is invalid")
    void login_ShouldReturnBadRequest_WhenBodyIsInvalid () throws Exception {

        LoginRequest request = new LoginRequest("invalid-email", "");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation error"))
            .andExpect(jsonPath("$.message", containsString("email")))
            .andExpect(jsonPath("$.message", containsString("password")));

        verify(authenticationManager, never()).authenticate(any());
        verify(tokenService, never()).generateToken(any(User.class));
        verify(refreshTokenService, never()).generateRefreshToken(any(User.class));

    }

    @Test
    @DisplayName("Should return unauthorized when login credentials are invalid")
    void login_ShouldReturnUnauthorized_WhenCredentialsAreInvalid () throws Exception {

        LoginRequest request = new LoginRequest("tiago@test.com", "wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Authentication Error"))
            .andExpect(jsonPath("$.message").value("Incorrect email or password"))
            .andExpect(jsonPath("$.path").value("/auth/login"));

        verify(tokenService, never()).generateToken(any(User.class));
        verify(refreshTokenService, never()).generateRefreshToken(any(User.class));

    }

    @Test
    @DisplayName("Should refresh access token when refresh token is valid")
    void refresh_ShouldReturnNewAccessToken_WhenRefreshTokenIsValid () throws Exception {

        TokenRefreshRequest request = new TokenRefreshRequest("refresh-token");
        User user = createUser(1L, "Tiago", "tiago@test.com", Role.USER);
        RefreshToken refreshToken = createRefreshToken("refresh-token", user, Instant.now().plusSeconds(3600));

        when(refreshTokenService.findByToken("refresh-token")).thenReturn(refreshToken);
        when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);
        when(tokenService.generateToken(user)).thenReturn("new-access-token");

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("new-access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

        verify(refreshTokenService).findByToken("refresh-token");
        verify(refreshTokenService).verifyExpiration(refreshToken);
        verify(tokenService).generateToken(user);

    }

    @Test
    @DisplayName("Should return bad request when refresh body is invalid")
    void refresh_ShouldReturnBadRequest_WhenBodyIsInvalid () throws Exception {

        TokenRefreshRequest request = new TokenRefreshRequest("");

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation error"))
            .andExpect(jsonPath("$.message", containsString("refreshToken")));

        verify(refreshTokenService, never()).findByToken(any());
        verify(tokenService, never()).generateToken(any(User.class));

    }

    @Test
    @DisplayName("Should return not found when refresh token does not exist")
    void refresh_ShouldReturnNotFound_WhenRefreshTokenDoesNotExist () throws Exception {

        TokenRefreshRequest request = new TokenRefreshRequest("missing-token");

        when(refreshTokenService.findByToken("missing-token"))
            .thenThrow(new ResourceNotFoundException("Refresh token not found"));

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Resource not found"))
            .andExpect(jsonPath("$.message").value("Refresh token not found"))
            .andExpect(jsonPath("$.path").value("/auth/refresh"));

        verify(refreshTokenService, never()).verifyExpiration(any(RefreshToken.class));
        verify(tokenService, never()).generateToken(any(User.class));

    }

    @Test
    @DisplayName("Should return bad request when refresh token is expired")
    void refresh_ShouldReturnBadRequest_WhenRefreshTokenIsExpired () throws Exception {

        TokenRefreshRequest request = new TokenRefreshRequest("expired-token");
        User user = createUser(1L, "Tiago", "tiago@test.com", Role.USER);
        RefreshToken refreshToken = createRefreshToken("expired-token", user, Instant.now().minusSeconds(3600));

        when(refreshTokenService.findByToken("expired-token")).thenReturn(refreshToken);
        when(refreshTokenService.verifyExpiration(refreshToken))
            .thenThrow(new BadRequestException("Refresh token expired"));

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Data Error"))
            .andExpect(jsonPath("$.message").value("Refresh token expired"))
            .andExpect(jsonPath("$.path").value("/auth/refresh"));

        verify(tokenService, never()).generateToken(any(User.class));

    }

    @Test
    @DisplayName("Should logout authenticated user from one session")
    void logout_ShouldRevokeRefreshToken_WhenUserIsAuthenticated () throws Exception {

        User user = createUser(1L, "Tiago", "tiago@test.com", Role.USER);
        TokenRefreshRequest request = new TokenRefreshRequest("refresh-token");

        authenticate("access-token", user);

        mockMvc.perform(post("/auth/logout")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        verify(refreshTokenService).revoke("refresh-token", user);

    }

    @Test
    @DisplayName("Should return unauthorized when logout request has no access token")
    void logout_ShouldReturnUnauthorized_WhenRequestHasNoAccessToken () throws Exception {

        TokenRefreshRequest request = new TokenRefreshRequest("refresh-token");

        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());

        verify(refreshTokenService, never()).revoke(any(), any());

    }

    @Test
    @DisplayName("Should logout authenticated user from all sessions")
    void logoutAll_ShouldRevokeAllRefreshTokens_WhenUserIsAuthenticated () throws Exception {

        User user = createUser(1L, "Tiago", "tiago@test.com", Role.USER);

        authenticate("access-token", user);

        mockMvc.perform(delete("/auth/logout-all")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNoContent());

        verify(refreshTokenService).revokeAll(user);

    }

    @Test
    @DisplayName("Should return unauthorized when logout all request has no access token")
    void logoutAll_ShouldReturnUnauthorized_WhenRequestHasNoAccessToken () throws Exception {

        mockMvc.perform(delete("/auth/logout-all"))
            .andExpect(status().isUnauthorized());

        verify(refreshTokenService, never()).revokeAll(any(User.class));

    }

    private void authenticate (String token, UserDetails user) {

        when(tokenService.validateToken(token)).thenReturn(user.getUsername());
        when(userRepository.findByEmail(user.getUsername())).thenReturn(user);

    }

    private User createUser (Long id, String name, String email, Role role) {

        User user = new User();

        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(role);
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
