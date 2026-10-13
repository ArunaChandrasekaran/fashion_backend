package com.fashion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Pattern(regexp = "^[0-9+\\-\\s]{8,20}$", message = "Enter a valid phone number") String phone,
            @NotBlank @Size(min = 8, max = 72, message = "Password must be at least 8 characters") String password,
            @NotBlank String confirmPassword
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record UserSummary(
            Long id,
            String fullName,
            String email,
            String phone,
            boolean enabled,
            Set<String> roles,
            Instant createdAt
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword,
            @NotBlank String confirmPassword
    ) {
    }

    public record MessageResponse(String message) {
    }

    public record CsrfResponse(String headerName, String parameterName) {
    }
}
