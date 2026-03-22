package com.boilerplate.springauth.dto.user;

import java.time.LocalDateTime;

import com.boilerplate.springauth.models.enums.Role;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User profile information returned by the API")
public record UserResponse(
    @Schema(description = "Unique identifier of the user", example = "1")
    Long id,
    @Schema(description = "Full name of the user", example = "Tiago Paulin")
    String name,
    @Schema(description = "Registered email address", example = "tiago.paulin@example.com")
    String email,
    @Schema(description = "Access level assigned to the user", example = "ROLE_USER")
    Role role,
    @Schema(description = "Status indicating if the account is enabled", example = "true")
    boolean active,
    @Schema(description = "Timestamp when the user account was created", example = "2026-03-22T16:00:00")
    LocalDateTime createdAt
) {}