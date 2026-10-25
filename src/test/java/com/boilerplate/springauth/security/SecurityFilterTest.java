package com.boilerplate.springauth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.models.enums.Role;
import com.boilerplate.springauth.repositories.UserRepository;

import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
public class SecurityFilterTest {

    @Mock
    private TokenService tokenService;
    @Mock
    private UserRepository userRepository;

    private SecurityFilter securityFilter;

    @BeforeEach
    void setUp () {

        securityFilter = new SecurityFilter(tokenService, userRepository);
        SecurityContextHolder.clearContext();

    }

    @AfterEach
    void tearDown () {

        SecurityContextHolder.clearContext();

    }

    @Test
    @DisplayName("Should continue filter chain without authentication when authorization header is missing")
    void doFilterInternal_ShouldContinueWithoutAuthentication_WhenAuthorizationHeaderIsMissing () throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        securityFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertSame(request, filterChain.getRequest());
        assertSame(response, filterChain.getResponse());
        verifyNoInteractions(tokenService);
        verifyNoInteractions(userRepository);

    }

    @Test
    @DisplayName("Should authenticate user when bearer token is valid")
    void doFilterInternal_ShouldAuthenticateUser_WhenBearerTokenIsValid () throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        User user = createUser();

        request.addHeader("Authorization", "Bearer valid-token");
        when(tokenService.validateToken("valid-token")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(user);

        securityFilter.doFilter(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        assertEquals(user, authentication.getPrincipal());
        assertTrue(authentication.isAuthenticated());
        assertEquals(1, authentication.getAuthorities().size());
        assertSame(request, filterChain.getRequest());
        assertSame(response, filterChain.getResponse());
        verify(tokenService, times(1)).validateToken("valid-token");
        verify(userRepository, times(1)).findByEmail(user.getEmail());

    }

    @Test
    @DisplayName("Should not authenticate user when token is invalid")
    void doFilterInternal_ShouldNotAuthenticateUser_WhenTokenIsInvalid () throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        request.addHeader("Authorization", "Bearer invalid-token");
        when(tokenService.validateToken("invalid-token")).thenReturn("");

        securityFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertSame(request, filterChain.getRequest());
        assertSame(response, filterChain.getResponse());
        verify(tokenService, times(1)).validateToken("invalid-token");
        verify(userRepository, never()).findByEmail("");

    }

    @Test
    @DisplayName("Should not authenticate user when token subject no longer exists")
    void doFilterInternal_ShouldNotAuthenticateUser_WhenUserDoesNotExist () throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        request.addHeader("Authorization", "Bearer old-email-token");
        when(tokenService.validateToken("old-email-token")).thenReturn("old@test.com");
        when(userRepository.findByEmail("old@test.com")).thenReturn(null);

        securityFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertSame(request, filterChain.getRequest());
        assertSame(response, filterChain.getResponse());

    }

    @Test
    @DisplayName("Should not authenticate user when account is deactivated")
    void doFilterInternal_ShouldNotAuthenticateUser_WhenUserIsDeactivated () throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        User user = createUser();
        user.setActive(false);

        request.addHeader("Authorization", "Bearer valid-token");
        when(tokenService.validateToken("valid-token")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(user);

        securityFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertSame(request, filterChain.getRequest());
        assertSame(response, filterChain.getResponse());

    }

    @Test
    @DisplayName("Should clear previous authentication before filtering request")
    void doFilterInternal_ShouldClearPreviousAuthentication_BeforeFilteringRequest () throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        User user = createUser();

        SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );

        securityFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertSame(request, filterChain.getRequest());
        assertSame(response, filterChain.getResponse());
        verifyNoInteractions(tokenService);
        verifyNoInteractions(userRepository);

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
