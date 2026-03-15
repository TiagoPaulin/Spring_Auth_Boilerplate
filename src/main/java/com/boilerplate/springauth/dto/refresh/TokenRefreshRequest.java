package com.boilerplate.springauth.dto.refresh;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
    @NotBlank String refreshToken
) {}
