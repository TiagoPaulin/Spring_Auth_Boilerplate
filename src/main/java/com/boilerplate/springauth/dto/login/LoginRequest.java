package com.boilerplate.springauth.dto.login;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Email @NotBlank String email,
    String password
) {}