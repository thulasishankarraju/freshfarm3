package com.example.freshfarm3.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// ── 1. Send OTP (registration or forgot-password) ──────────────────────────
// Frontend sends: recipient (email or phone), channel, purpose, and role
@Data
public class SendOtpRequest {

    // Either email address OR phone number with country code e.g. "+919876543210"
    @NotBlank(message = "Email or phone number is required")
    private String recipient;

    // "EMAIL" or "PHONE"
    @NotBlank(message = "Channel is required (EMAIL or PHONE)")
    private String channel;

    // "REGISTRATION" or "FORGOT_PASSWORD"
    @NotBlank(message = "Purpose is required")
    private String purpose;

    // "BUYER" or "FARMER" — required for FORGOT_PASSWORD so we look in the right table
    private String role;
}


// ── 2. Verify OTP ──────────────────────────────────────────────────────────
@Data
class VerifyOtpRequest {

    @NotBlank(message = "Recipient is required")
    private String recipient;   // same email or phone used in SendOtpRequest

    @NotBlank(message = "OTP code is required")
    private String otpCode;     // 6-digit code the user typed

    // "REGISTRATION" or "FORGOT_PASSWORD"
    @NotBlank(message = "Purpose is required")
    private String purpose;
}


// ── 3. Register (called after OTP verified) ────────────────────────────────
@Data
class RegisterRequest {

    @NotBlank private String name;
    private String email;       // optional if phone was used for OTP
    private String phone;       // optional if email was used for OTP
    @NotBlank private String password;
    @NotBlank private String role;  // "BUYER" or "FARMER"

    // Farmer-specific fields (ignored for buyers)
    private String aadhaarNumber;
    private String bankAccountNumber;
    private String ifscCode;
    private String farmName;
    private String farmLocation;
}


// ── 4. Login ───────────────────────────────────────────────────────────────
@Data
class LoginRequest {

    // User can log in with email OR phone — whichever they registered with
    private String email;
    private String phone;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Role is required")
    private String role;    // "BUYER" or "FARMER"
}


// ── 5. Reset Password (called after OTP verified for forgot-password) ──────
@Data
class ResetPasswordRequest {

    @NotBlank(message = "Recipient is required")
    private String recipient;  // email or phone

    @NotBlank(message = "OTP code is required")
    private String otpCode;

    @NotBlank(message = "New password is required")
    private String newPassword;

    @NotBlank(message = "Role is required")
    private String role;       // "BUYER" or "FARMER"
}
