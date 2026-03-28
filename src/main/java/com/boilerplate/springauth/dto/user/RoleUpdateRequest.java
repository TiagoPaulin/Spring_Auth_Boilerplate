package com.boilerplate.springauth.dto.user;

import com.boilerplate.springauth.models.enums.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for updating a user's access level (Role)")
public record RoleUpdateRequest(
    @Schema(description = "THe new role to be assigned", example = "ADMIN", allowableValues = {"USER", "ADMIN"})
    @NotNull(message = "Role is required")
    Role role
) {}