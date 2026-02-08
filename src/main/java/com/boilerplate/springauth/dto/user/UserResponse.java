package com.boilerplate.springauth.dto.user;

import java.time.LocalDateTime;

import com.boilerplate.springauth.models.enums.Role;

public record UserResponse(
    Long id,
    String name,
    String email,
    Role role,
    boolean active,
    LocalDateTime createdAt
) {}