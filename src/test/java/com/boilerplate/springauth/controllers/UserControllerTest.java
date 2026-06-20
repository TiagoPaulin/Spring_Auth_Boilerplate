package com.boilerplate.springauth.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.boilerplate.springauth.dto.user.RoleUpdateRequest;
import com.boilerplate.springauth.dto.user.UserCreateRequest;
import com.boilerplate.springauth.dto.user.UserResponse;
import com.boilerplate.springauth.dto.user.UserUpdateRequest;
import com.boilerplate.springauth.exceptions.BadRequestException;
import com.boilerplate.springauth.exceptions.ResourceNotFoundException;
import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.models.enums.Role;
import com.boilerplate.springauth.repositories.UserRepository;
import com.boilerplate.springauth.security.TokenService;
import com.boilerplate.springauth.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private TokenService tokenService;
    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("Should create user and return created status")
    void create_ShouldCreateUserAndReturnCreatedStatus () throws Exception {

        UserCreateRequest request = new UserCreateRequest("Tiago", "tiago@test.com", "password123");
        UserResponse response = createUserResponse(1L, "Tiago", "tiago@test.com", Role.USER, true);

        when(userService.create(request)).thenReturn(response);

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/users/1")))
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Tiago"))
            .andExpect(jsonPath("$.email").value("tiago@test.com"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.active").value(true));

        verify(userService).create(request);

    }

    @Test
    @DisplayName("Should return bad request when creating user with invalid body")
    void create_ShouldReturnBadRequest_WhenBodyIsInvalid () throws Exception {

        UserCreateRequest request = new UserCreateRequest("", "invalid-email", "123");

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation error"))
            .andExpect(jsonPath("$.message", containsString("name")))
            .andExpect(jsonPath("$.message", containsString("email")))
            .andExpect(jsonPath("$.message", containsString("password")));

        verify(userService, never()).create(any(UserCreateRequest.class));

    }

    @Test
    @DisplayName("Should return bad request when service rejects user creation")
    void create_ShouldReturnBadRequest_WhenServiceRejectsCreation () throws Exception {

        UserCreateRequest request = new UserCreateRequest("Tiago", "tiago@test.com", "password123");

        when(userService.create(request)).thenThrow(new BadRequestException("Email already in use"));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Data Error"))
            .andExpect(jsonPath("$.message").value("Email already in use"))
            .andExpect(jsonPath("$.path").value("/users"));

    }

    @Test
    @DisplayName("Should return paginated users when authenticated user is admin")
    void findAll_ShouldReturnPaginatedUsers_WhenUserIsAdmin () throws Exception {

        User admin = createUser(1L, "Admin", "admin@test.com", Role.ADMIN);
        UserResponse userResponse = createUserResponse(2L, "Tiago", "tiago@test.com", Role.USER, true);
        var pageable = PageRequest.of(0, 20);

        authenticate("admin-token", admin);
        when(userService.findAll(pageable)).thenReturn(new PageImpl<>(List.of(userResponse), pageable, 1));

        mockMvc.perform(get("/users")
                .header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(2L))
            .andExpect(jsonPath("$.content[0].name").value("Tiago"))
            .andExpect(jsonPath("$.content[0].email").value("tiago@test.com"))
            .andExpect(jsonPath("$.content[0].role").value("USER"))
            .andExpect(jsonPath("$.totalElements").value(1));

        verify(userService).findAll(pageable);

    }

    @Test
    @DisplayName("Should return forbidden when non admin tries to list users")
    void findAll_ShouldReturnForbidden_WhenUserIsNotAdmin () throws Exception {

        User user = createUser(1L, "Tiago", "tiago@test.com", Role.USER);

        authenticate("user-token", user);

        mockMvc.perform(get("/users")
                .header("Authorization", "Bearer user-token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.error").value("Access Denied"));

        verify(userService, never()).findAll(any());

    }

    @Test
    @DisplayName("Should return unauthorized when request has no token")
    void findAll_ShouldReturnUnauthorized_WhenRequestHasNoToken () throws Exception {

        mockMvc.perform(get("/users"))
            .andExpect(status().isUnauthorized());

        verify(userService, never()).findAll(any());

    }

    @Test
    @DisplayName("Should return user when authenticated user is owner")
    void find_ShouldReturnUser_WhenAuthenticatedUserIsOwner () throws Exception {

        User user = createUser(1L, "Tiago", "tiago@test.com", Role.USER);
        UserResponse response = createUserResponse(1L, "Tiago", "tiago@test.com", Role.USER, true);

        authenticate("user-token", user);
        when(userService.findUser(1L)).thenReturn(response);

        mockMvc.perform(get("/users/1")
                .header("Authorization", "Bearer user-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.email").value("tiago@test.com"));

        verify(userService).findUser(1L);

    }

    @Test
    @DisplayName("Should return user when authenticated user is admin")
    void find_ShouldReturnUser_WhenAuthenticatedUserIsAdmin () throws Exception {

        User admin = createUser(1L, "Admin", "admin@test.com", Role.ADMIN);
        UserResponse response = createUserResponse(2L, "Tiago", "tiago@test.com", Role.USER, true);

        authenticate("admin-token", admin);
        when(userService.findUser(2L)).thenReturn(response);

        mockMvc.perform(get("/users/2")
                .header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(2L))
            .andExpect(jsonPath("$.email").value("tiago@test.com"));

        verify(userService).findUser(2L);

    }

    @Test
    @DisplayName("Should return forbidden when authenticated user is not owner")
    void find_ShouldReturnForbidden_WhenAuthenticatedUserIsNotOwner () throws Exception {

        User user = createUser(1L, "Tiago", "tiago@test.com", Role.USER);

        authenticate("user-token", user);

        mockMvc.perform(get("/users/2")
                .header("Authorization", "Bearer user-token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.error").value("Access Denied"));

        verify(userService, never()).findUser(2L);

    }

    @Test
    @DisplayName("Should return not found when service cannot find user")
    void find_ShouldReturnNotFound_WhenUserDoesNotExist () throws Exception {

        User admin = createUser(1L, "Admin", "admin@test.com", Role.ADMIN);

        authenticate("admin-token", admin);
        when(userService.findUser(99L)).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/users/99")
                .header("Authorization", "Bearer admin-token"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Resource not found"))
            .andExpect(jsonPath("$.message").value("User not found"))
            .andExpect(jsonPath("$.path").value("/users/99"));

    }

    @Test
    @DisplayName("Should delete user when authenticated user is admin")
    void delete_ShouldDeleteUser_WhenAuthenticatedUserIsAdmin () throws Exception {

        User admin = createUser(1L, "Admin", "admin@test.com", Role.ADMIN);

        authenticate("admin-token", admin);

        mockMvc.perform(delete("/users/2")
                .header("Authorization", "Bearer admin-token"))
            .andExpect(status().isNoContent());

        verify(userService).delete(2L);

    }

    @Test
    @DisplayName("Should return forbidden when non admin tries to delete user")
    void delete_ShouldReturnForbidden_WhenUserIsNotAdmin () throws Exception {

        User user = createUser(1L, "Tiago", "tiago@test.com", Role.USER);

        authenticate("user-token", user);

        mockMvc.perform(delete("/users/2")
                .header("Authorization", "Bearer user-token"))
            .andExpect(status().isForbidden());

        verify(userService, never()).delete(2L);

    }

    @Test
    @DisplayName("Should update user when authenticated user is owner")
    void update_ShouldUpdateUser_WhenAuthenticatedUserIsOwner () throws Exception {

        User user = createUser(1L, "Tiago", "tiago@test.com", Role.USER);
        UserUpdateRequest request = new UserUpdateRequest("Tiago Updated", "updated@test.com", null);
        UserResponse response = createUserResponse(1L, "Tiago Updated", "updated@test.com", Role.USER, true);

        authenticate("user-token", user);
        when(userService.update(1L, request)).thenReturn(response);

        mockMvc.perform(put("/users/1")
                .header("Authorization", "Bearer user-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Tiago Updated"))
            .andExpect(jsonPath("$.email").value("updated@test.com"));

        verify(userService).update(1L, request);

    }

    @Test
    @DisplayName("Should return forbidden when non owner tries to update user")
    void update_ShouldReturnForbidden_WhenAuthenticatedUserIsNotOwner () throws Exception {

        User user = createUser(1L, "Tiago", "tiago@test.com", Role.USER);
        UserUpdateRequest request = new UserUpdateRequest("Another", null, null);

        authenticate("user-token", user);

        mockMvc.perform(put("/users/2")
                .header("Authorization", "Bearer user-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());

        verify(userService, never()).update(any(), any());

    }

    @Test
    @DisplayName("Should activate user when authenticated user is admin")
    void activate_ShouldActivateUser_WhenAuthenticatedUserIsAdmin () throws Exception {

        User admin = createUser(1L, "Admin", "admin@test.com", Role.ADMIN);

        authenticate("admin-token", admin);

        mockMvc.perform(patch("/users/2/activate")
                .header("Authorization", "Bearer admin-token"))
            .andExpect(status().isNoContent());

        verify(userService).activate(2L);

    }

    @Test
    @DisplayName("Should deactivate user when authenticated user is admin")
    void deactivate_ShouldDeactivateUser_WhenAuthenticatedUserIsAdmin () throws Exception {

        User admin = createUser(1L, "Admin", "admin@test.com", Role.ADMIN);

        authenticate("admin-token", admin);

        mockMvc.perform(patch("/users/2/deactivate")
                .header("Authorization", "Bearer admin-token"))
            .andExpect(status().isNoContent());

        verify(userService).deactivate(2L);

    }

    @Test
    @DisplayName("Should change user role when authenticated user is admin")
    void changeRole_ShouldChangeUserRole_WhenAuthenticatedUserIsAdmin () throws Exception {

        User admin = createUser(1L, "Admin", "admin@test.com", Role.ADMIN);
        RoleUpdateRequest request = new RoleUpdateRequest(Role.ADMIN);

        authenticate("admin-token", admin);

        mockMvc.perform(patch("/users/2/role")
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        verify(userService).changeRole(2L, request);

    }

    @Test
    @DisplayName("Should return forbidden when non admin tries to change role")
    void changeRole_ShouldReturnForbidden_WhenUserIsNotAdmin () throws Exception {

        User user = createUser(1L, "Tiago", "tiago@test.com", Role.USER);
        RoleUpdateRequest request = new RoleUpdateRequest(Role.ADMIN);

        authenticate("user-token", user);

        mockMvc.perform(patch("/users/2/role")
                .header("Authorization", "Bearer user-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());

        verify(userService, never()).changeRole(any(), any());

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

    private UserResponse createUserResponse (Long id, String name, String email, Role role, boolean active) {

        return new UserResponse(
            id,
            name,
            email,
            role,
            active,
            LocalDateTime.of(2026, 3, 22, 16, 0),
            LocalDateTime.of(2026, 3, 22, 17, 0)
        );

    }

}
