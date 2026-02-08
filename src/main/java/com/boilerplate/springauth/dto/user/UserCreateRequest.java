package com.boilerplate.springauth.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserCreateRequest(
    @NotBlank String name,
    @Email @NotBlank String email,
    @NotBlank String password
) {}