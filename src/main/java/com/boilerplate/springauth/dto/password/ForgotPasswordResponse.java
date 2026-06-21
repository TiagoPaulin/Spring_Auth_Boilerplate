package com.boilerplate.springauth.dto.password;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response object containing reset password token")
public record ForgotPasswordResponse(
    @Schema(description = "Thes reset password token", example = "550e8400-e29b-41d4-a716-446655440000")
    String token
) {}