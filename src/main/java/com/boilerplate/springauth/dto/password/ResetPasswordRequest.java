package com.boilerplate.springauth.dto.password;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for reset user password")
public record ResetPasswordRequest(
    @Schema(description = "The unique UUID-formated reset password token")
    @NotBlank(message = "Reset password token is required")
    String resetToken,
    @Schema(description = "The new user's password", example = "newSecurePass123")
    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "New password must be at least 6 characters long")
    String newPassword
) {}