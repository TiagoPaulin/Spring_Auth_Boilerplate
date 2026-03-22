package com.boilerplate.springauth.dto.login;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials required to authenticate and generate access tokens")
public record LoginRequest(
    @Schema(description = "Registered user's email address", example = "admin@system.com")
    @Email(message = "Must be a well-formed email address") 
    @NotBlank(message = "Email is required")
    String email,
    @Schema(description = "User's secret password", example = "admin")
    @NotBlank(message = "Password is required")
    String password
) {}