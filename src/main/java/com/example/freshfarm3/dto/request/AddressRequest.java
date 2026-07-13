package com.example.freshfarm3.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * AddressRequest — Payload for creating/updating a buyer's delivery address.
 *
 * Used by: POST /api/addresses
 *          PUT  /api/addresses/{id}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be 2–100 characters")
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
    private String mobileNumber;

    @NotBlank(message = "Address line is required")
    @Size(max = 300)
    private String addressLine;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Enter a valid 6-digit pincode")
    private String pincode;

    @Builder.Default
    private boolean isDefault = false;

    // Optional — filled in automatically by "Use my current location" on
    // the address form. Enables distance-based delivery pricing; if
    // omitted, delivery charge falls back to a flat rate.
    private Double latitude;
    private Double longitude;
}