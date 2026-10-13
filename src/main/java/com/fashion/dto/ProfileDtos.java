package com.fashion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Pattern(regexp = "^[0-9+\\-\\s]{8,20}$", message = "Enter a valid phone number") String phone
    ) {
    }

    public record AddressRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Size(max = 20) String phone,
            @NotBlank @Size(max = 200) String line1,
            @Size(max = 200) String line2,
            @NotBlank @Size(max = 80) String city,
            @NotBlank @Size(max = 80) String state,
            @NotBlank @Size(max = 20) String postalCode,
            @Size(max = 80) String country,
            boolean defaultAddress
    ) {
    }

    public record AddressResponse(
            Long id,
            String fullName,
            String phone,
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country,
            boolean defaultAddress
    ) {
    }

    public record ProfileResponse(
            Long id,
            String fullName,
            String email,
            String phone,
            Instant createdAt,
            long orderCount,
            BigDecimal totalSpent,
            List<AddressResponse> addresses
    ) {
    }

    public record CustomerSummary(
            Long id,
            String fullName,
            String email,
            String phone,
            boolean enabled,
            Instant createdAt,
            long orderCount,
            BigDecimal totalSpent
    ) {
    }

    public record CustomerStatusRequest(boolean enabled) {
    }
}
