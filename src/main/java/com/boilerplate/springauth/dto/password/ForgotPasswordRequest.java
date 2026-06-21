package com.boilerplate.springauth.dto.password;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for obtaining a new reset password Token")
public record ForgotPasswordRequest(
    @Schema(description = "User registered email", example = "tiago.paulin@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "must be a well-formed email address")
    String email
) {}