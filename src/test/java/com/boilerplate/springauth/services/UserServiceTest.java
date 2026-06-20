package com.boilerplate.springauth.services;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.boilerplate.springauth.dto.user.RoleUpdateRequest;
import com.boilerplate.springauth.dto.user.UserCreateRequest;
import com.boilerplate.springauth.dto.user.UserUpdateRequest;
import com.boilerplate.springauth.exceptions.BadRequestException;
import com.boilerplate.springauth.exceptions.ResourceNotFoundException;
import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.models.enums.Role;
import com.boilerplate.springauth.repositories.UserRepository;
import com.boilerplate.springauth.security.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository repository;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should return paginated users")
    void findAll_ShouldReturnPaginatedUsers () {

        var pageable = PageRequest.of(0, 10);
        User user = createUser(1L, "Tiago", "tiago@test.com", Role.USER, true);

        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        var response = userService.findAll(pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals("Tiago", response.getContent().get(0).name());
        assertEquals("tiago@test.com", response.getContent().get(0).email());
        verify(repository, times(1)).findAll(pageable);

    }

    @Test
    @DisplayName("Should create a user when email is available")
    void create_ShouldCreateUser_WhenEmailIsAvailable () {

        UserCreateRequest request = new UserCreateRequest("Tiago", "tiago@test.com", "password123");

        when(repository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        var response = userService.create(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(1L, response.id());
        assertEquals("Tiago", response.name());
        assertEquals("tiago@test.com", response.email());
        assertEquals(Role.USER, response.role());
        assertTrue(response.active());
        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals(Role.USER, savedUser.getRole());
        assertTrue(savedUser.isActive());

    }

    @Test
    @DisplayName("Should throw BadRequestException when creating user with duplicated email")
    void create_ShouldThrowException_WhenEmailAlreadyExists () {

        UserCreateRequest request = new UserCreateRequest("Tiago", "tiago@test.com", "password123");

        when(repository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> {
            userService.create(request);
        });

        verify(repository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);

    }

    @Test
    @DisplayName("Should return a user when ID exists")
    void findUser_ShouldReturnUser_WhenIdExists () {

        Long userId = 1L;
        User user = createUser(userId, "Tiago", "tiago@test.com", Role.USER, true);

        when(repository.findById(userId)).thenReturn(Optional.of(user));

        var response = userService.findUser(userId);

        assertNotNull(response);
        assertEquals("Tiago", response.name());
        assertEquals("tiago@test.com", response.email());
        verify(repository, times(1)).findById(userId);

    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user does not exist")
    void findUser_ShouldThrowException_WhenIdDoesNotExist () {

        Long userId = 99L;
        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.findUser(userId);
        });

    }

    @Test
    @DisplayName("Should delete user and revoke refresh tokens")
    void delete_ShouldDeleteUserAndRevokeTokens_WhenUserExists () {

        Long userId = 1L;
        User user = createUser(userId, "Tiago", "tiago@test.com", Role.USER, true);

        when(repository.findById(userId)).thenReturn(Optional.of(user));

        userService.delete(userId);

        verify(refreshTokenService, times(1)).revokeAll(user);
        verify(repository, times(1)).deleteById(userId);

    }

    @Test
    @DisplayName("Should delete admin when it is not the last admin")
    void delete_ShouldDeleteAdmin_WhenItIsNotLastAdmin () {

        Long userId = 1L;
        User user = createUser(userId, "Admin", "admin@test.com", Role.ADMIN, true);

        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.countByRole(Role.ADMIN)).thenReturn(2L);

        userService.delete(userId);

        verify(refreshTokenService, times(1)).revokeAll(user);
        verify(repository, times(1)).deleteById(userId);

    }

    @Test
    @DisplayName("Should throw BadRequestException when deleting last admin")
    void delete_ShouldThrowException_WhenDeletingLastAdmin () {

        Long userId = 1L;
        User user = createUser(userId, "Admin", "admin@test.com", Role.ADMIN, true);

        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThrows(BadRequestException.class, () -> {
            userService.delete(userId);
        });

        verify(refreshTokenService, never()).revokeAll(any(User.class));
        verify(repository, never()).deleteById(userId);

    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting user that does not exist")
    void delete_ShouldThrowException_WhenUserDoesNotExist () {

        Long userId = 99L;
        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.delete(userId);
        });

        verify(repository, never()).deleteById(userId);
        verifyNoInteractions(refreshTokenService);

    }

    @Test
    @DisplayName("Should update user name without revoking tokens")
    void update_ShouldUpdateNameWithoutRevokingTokens_WhenOnlyNameChanges () {

        Long userId = 1L;
        User user = createUser(userId, "Tiago", "tiago@test.com", Role.USER, true);
        UserUpdateRequest request = new UserUpdateRequest("Tiago Updated", null, null);

        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);

        var response = userService.update(userId, request);

        assertEquals("Tiago Updated", response.name());
        assertEquals("tiago@test.com", response.email());
        verify(refreshTokenService, never()).revokeAll(any(User.class));
        verify(passwordEncoder, never()).encode(any());
        verify(repository, times(1)).save(user);

    }

    @Test
    @DisplayName("Should update email and revoke tokens")
    void update_ShouldUpdateEmailAndRevokeTokens_WhenEmailChanges () {

        Long userId = 1L;
        User user = createUser(userId, "Tiago", "tiago@test.com", Role.USER, true);
        UserUpdateRequest request = new UserUpdateRequest(null, "new@test.com", null);

        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.existsByEmail(request.email())).thenReturn(false);
        when(repository.save(user)).thenReturn(user);

        var response = userService.update(userId, request);

        assertEquals("new@test.com", response.email());
        verify(refreshTokenService, times(1)).revokeAll(user);
        verify(repository, times(1)).save(user);

    }

    @Test
    @DisplayName("Should update password and revoke tokens")
    void update_ShouldUpdatePasswordAndRevokeTokens_WhenPasswordChanges () {

        Long userId = 1L;
        User user = createUser(userId, "Tiago", "tiago@test.com", Role.USER, true);
        UserUpdateRequest request = new UserUpdateRequest(null, null, "newPassword123");

        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(request.password())).thenReturn("new-encoded-password");
        when(repository.save(user)).thenReturn(user);

        userService.update(userId, request);

        assertEquals("new-encoded-password", user.getPassword());
        verify(refreshTokenService, times(1)).revokeAll(user);
        verify(repository, times(1)).save(user);

    }

    @Test
    @DisplayName("Should throw BadRequestException when updating to duplicated email")
    void update_ShouldThrowException_WhenEmailAlreadyExists () {

        Long userId = 1L;
        User user = createUser(userId, "Tiago", "tiago@test.com", Role.USER, true);
        UserUpdateRequest request = new UserUpdateRequest(null, "duplicated@test.com", null);

        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> {
            userService.update(userId, request);
        });

        verify(repository, never()).save(any(User.class));
        verify(refreshTokenService, never()).revokeAll(any(User.class));

    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating user that does not exist")
    void update_ShouldThrowException_WhenUserDoesNotExist () {

        Long userId = 99L;
        UserUpdateRequest request = new UserUpdateRequest("Tiago", null, null);

        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.update(userId, request);
        });

        verify(repository, never()).save(any(User.class));

    }

    @Test
    @DisplayName("Should activate user")
    void activate_ShouldActivateUser_WhenUserExists () {

        Long userId = 1L;
        User user = createUser(userId, "Tiago", "tiago@test.com", Role.USER, false);

        when(repository.findById(userId)).thenReturn(Optional.of(user));

        userService.activate(userId);

        assertTrue(user.isActive());
        verify(repository, times(1)).save(user);
        verifyNoInteractions(refreshTokenService);

    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when activating user that does not exist")
    void activate_ShouldThrowException_WhenUserDoesNotExist () {

        Long userId = 99L;
        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.activate(userId);
        });

        verify(repository, never()).save(any(User.class));

    }

    @Test
    @DisplayName("Should deactivate user and revoke tokens")
    void deactivate_ShouldDeactivateUserAndRevokeTokens_WhenUserExists () {

        Long userId = 1L;
        User user = createUser(userId, "Tiago", "tiago@test.com", Role.USER, true);

        when(repository.findById(userId)).thenReturn(Optional.of(user));

        userService.deactivate(userId);

        assertFalse(user.isActive());
        verify(repository, times(1)).save(user);
        verify(refreshTokenService, times(1)).revokeAll(user);

    }

    @Test
    @DisplayName("Should throw BadRequestException when deactivating last admin")
    void deactivate_ShouldThrowException_WhenDeactivatingLastAdmin () {

        Long userId = 1L;
        User user = createUser(userId, "Admin", "admin@test.com", Role.ADMIN, true);

        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThrows(BadRequestException.class, () -> {
            userService.deactivate(userId);
        });

        assertTrue(user.isActive());
        verify(repository, never()).save(any(User.class));
        verify(refreshTokenService, never()).revokeAll(any(User.class));

    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deactivating user that does not exist")
    void deactivate_ShouldThrowException_WhenUserDoesNotExist () {

        Long userId = 99L;
        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.deactivate(userId);
        });

        verify(repository, never()).save(any(User.class));
        verifyNoInteractions(refreshTokenService);

    }

    @Test
    @DisplayName("Should change user role and revoke tokens")
    void changeRole_ShouldChangeRoleAndRevokeTokens_WhenUserExists () {

        Long userId = 1L;
        User user = createUser(userId, "Tiago", "tiago@test.com", Role.USER, true);
        RoleUpdateRequest request = new RoleUpdateRequest(Role.ADMIN);

        when(repository.findById(userId)).thenReturn(Optional.of(user));

        userService.changeRole(userId, request);

        assertEquals(Role.ADMIN, user.getRole());
        verify(repository, times(1)).save(user);
        verify(refreshTokenService, times(1)).revokeAll(user);

    }

    @Test
    @DisplayName("Should throw BadRequestException when changing last admin role")
    void changeRole_ShouldThrowException_WhenChangingLastAdminRole () {

        Long userId = 1L;
        User user = createUser(userId, "Admin", "admin@test.com", Role.ADMIN, true);
        RoleUpdateRequest request = new RoleUpdateRequest(Role.USER);

        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThrows(BadRequestException.class, () -> {
            userService.changeRole(userId, request);
        });

        assertEquals(Role.ADMIN, user.getRole());
        verify(repository, never()).save(any(User.class));
        verify(refreshTokenService, never()).revokeAll(any(User.class));

    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when changing role of user that does not exist")
    void changeRole_ShouldThrowException_WhenUserDoesNotExist () {

        Long userId = 99L;
        RoleUpdateRequest request = new RoleUpdateRequest(Role.ADMIN);

        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.changeRole(userId, request);
        });

        verify(repository, never()).save(any(User.class));
        verifyNoInteractions(refreshTokenService);

    }

    private User createUser (Long id, String name, String email, Role role, boolean active) {

        User user = new User();

        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setActive(active);
        user.setCreatedAt(LocalDateTime.of(2026, 3, 22, 16, 0));
        user.setUpdatedAt(LocalDateTime.of(2026, 3, 22, 17, 0));

        return user;

    }

}
