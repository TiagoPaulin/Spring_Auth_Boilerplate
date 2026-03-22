package com.boilerplate.springauth.dto.refresh;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for obtaining a new Access Token using a valid Refresh Token")
public record TokenRefreshRequest(
    @Schema(description = "The unique UUID-formatted Refresh Token stored in the database. Used to identify the session.", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {}
