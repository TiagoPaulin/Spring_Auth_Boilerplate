package com.boilerplate.springauth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Data required to register a new user in the system")
public record UserCreateRequest(
    @Schema(description = "The user's full name", example = "Tiago Paulin")
    @NotBlank(message = "Name is required")
    String name,
    @Schema(description = "A unique and valid email address to be used as login identifier", example = "tiago.paulin@example.com")
    @Email(message = "must be a well-formed email address")
    @NotBlank(message = "Email is required")
    String email,
    @Schema(description = "The user's password. It will be safely hashed before storage", example = "securePass123")
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    String password
) {}