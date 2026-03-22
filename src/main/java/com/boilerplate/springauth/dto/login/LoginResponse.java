package com.boilerplate.springauth.dto.login;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response object containing authentication tokens after a successful login")
public record LoginResponse(
    @Schema(description = "The short-lived JWT Access Token. Must be included in the Authorization header for protected requests.", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String token,
    @Schema(description = "The long-lived Refresh Token (UUID). Used to obtain a new Access Token when the current one expires.", example = "550e8400-e29b-41d4-a716-446655440000")
    String refreshToken
) {}