package com.boilerplate.springauth.controllers;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.boilerplate.springauth.dto.user.RoleUpdateRequest;
import com.boilerplate.springauth.dto.user.UserCreateRequest;
import com.boilerplate.springauth.dto.user.UserResponse;
import com.boilerplate.springauth.dto.user.UserUpdateRequest;
import com.boilerplate.springauth.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/users")
@Tag(name = "Users", description = "Management of user profiles and account statuses")
public class UserController {

    private final UserService service;

    public UserController (UserService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (Paginated)", description = "Retrieves a paginated list of all registered users. Restricted to Admins.")
    public ResponseEntity<Page<UserResponse>> findAll(@ParameterObject Pageable pageable) {
        Page<UserResponse> list = service.findAll(pageable);
        return ResponseEntity.ok().body(list);
    }

    @PostMapping
    @Operation(summary = "Register a new user", description = "Creates a new user account in the system. Publicly accessible.")
    public ResponseEntity<UserResponse> create (@Valid @RequestBody UserCreateRequest request) {

        UserResponse obj = service.create(request);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                    .buildAndExpand(obj.id()).toUri();
        
        return ResponseEntity.created(uri).body(obj);

    }

    @GetMapping(value = "/{id}")
    @PreAuthorize("@authorizationService.isOwner(#id) or hasRole('ADMIN')")
    @Operation(summary = "Get user by ID", description = "Retrieves profile details. Accessible by account owner or Admins.")
    public ResponseEntity<UserResponse> find (@PathVariable Long id) {

        UserResponse user = service.findUser(id);

        return ResponseEntity.ok().body(user);

    }
   
    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Delete user", description = "Permanently removes a user from the database. Restricted to Admins.")
    public ResponseEntity<Void> delete (@PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.noContent().build();

    }

    @PutMapping(value = "/{id}")
    @PreAuthorize("@authorizationService.isOwner(#id) or hasRole('ADMIN')")
    @Operation(summary = "Update user profile", description = "Updates personal information (name, email, password). Accessible by account owner or Admins.")
    public ResponseEntity<UserResponse> update (@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {

        UserResponse response = service.update(id, request);

        return ResponseEntity.ok().body(response);

    }

    @PatchMapping(value = "/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate user account", description = "Re-enables a disabled account. Restricted to Admins.")
    public ResponseEntity<Void> activate (@PathVariable Long id) {

        service.activate(id);

        return ResponseEntity.noContent().build();

    }

    @PatchMapping(value = "/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate user account", description = "Disables a user account, preventing login. Restricted to Admins.")
    public ResponseEntity<Void> deactivate (@PathVariable Long id) {

        service.deactivate(id);

        return ResponseEntity.noContent().build();

    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change user role", description = "Updates the access level of a user. Restricted to Admins.")
    public ResponseEntity<Void> changeRole (@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {

        service.changeRole(id, request);

        return ResponseEntity.noContent().build();

    }

}
