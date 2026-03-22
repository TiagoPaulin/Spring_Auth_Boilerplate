package com.boilerplate.springauth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "Data used to partially or fully update user profile information")
public record UserUpdateRequest(
    @Schema(description = "Updated full name of the user", example = "Tiago Paulin Updated")
    String name,
    @Schema(description = "New email address (must be unique)", example = "tiago.updated@example.com")
    @Email(message = "Must be a well-formed email address")
    String email,
    @Schema(description = "New password for the account (leave blank if you don't want to change)", example = "newStrongPassword123")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    String password,
    @Schema(description = "Account status (set to false to disable the user)", example = "true")
    Boolean active
) {}
