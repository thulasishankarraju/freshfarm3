package com.example.freshfarm3.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * RegisterRequestDto — Payload for buyer and shop registration.
 *
 * Used by: POST /api/auth/register/buyer
 *          POST /api/auth/register/shop
 *
 * Validation annotations throw MethodArgumentNotValidException
 * if any field fails — Spring returns 400 Bad Request automatically.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    // ── Common Fields (Buyer + Shop) ──

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be 2–100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Please provide a valid 10-digit Indian mobile number")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // ── Shop-Only Fields (optional for buyers, validated in service) ──

    private String shopName;

    private String village;

    private String district;

    private String state;

    private String pincode;

    private String aadhaarNumber;

    private String bankAccountNumber;

    private String ifscCode;

    private String bio;
}