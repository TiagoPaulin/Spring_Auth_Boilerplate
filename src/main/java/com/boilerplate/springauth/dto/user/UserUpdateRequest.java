package com.boilerplate.springauth.dto.user;

import jakarta.validation.constraints.Email;

public record UserUpdateRequest(
    String name,
    @Email String email,
    String password,
    Boolean active
) {}
