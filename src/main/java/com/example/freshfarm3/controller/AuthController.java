package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.response.AuthResponse;
import com.example.freshfarm3.dto.request.LoginRequest;
import com.example.freshfarm3.dto.request.RegisterRequest;
import com.example.freshfarm3.dto.request.SendOtpRequest;
import com.example.freshfarm3.dto.request.VerifyOtpRequest;
import com.example.freshfarm3.dto.request.ResetPasswordRequest;
import com.example.freshfarm3.service.AuthService;
import com.example.freshfarm3.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AuthController — Public REST endpoints for authentication.
 *
 * Endpoints:
 *   POST /api/auth/register/buyer    → Register a new buyer
 *   POST /api/auth/register/shop   → Register a new shop
 *   POST /api/auth/login             → Login with email + password
 *
 * All endpoints are PUBLIC (no JWT required) — configured in SecurityConfig.
 *
 * Rule: Controller NEVER touches Repository directly.
 *       Controller → Service → Repository (always)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService  otpService;

    // ─────────────────────────────────────
    // BUYER REGISTRATION
    // ─────────────────────────────────────

    /**
     * Register a new Buyer.
     *
     * Request Body (JSON):
     * {
     *   "fullName": "Ravi Kumar",
     *   "email": "ravi@example.com",
     *   "phone": "9876543210",
     *   "password": "MyPass123"
     * }
     *
     * Response (201 Created):
     * {
     *   "token": "eyJhbGci...",
     *   "tokenType": "Bearer",
     *   "userId": 1,
     *   "fullName": "Ravi Kumar",
     *   "email": "ravi@example.com",
     *   "role": "BUYER",
     *   "expiresIn": 86400000
     * }
     */
    @PostMapping("/register/buyer")
    public ResponseEntity<?> registerBuyer(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.registerBuyer(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────
    // SHOP REGISTRATION
    // ─────────────────────────────────────

    /**
     * Register a new Shop.
     *
     * Request Body (JSON):
     * {
     *   "fullName": "Suresh Patel",
     *   "email": "suresh@shop.com",
     *   "phone": "9123456780",
     *   "password": "ShopPass123",
     *   "shopName": "Green Fields Store",
     *   "village": "Vangoor",
     *   "district": "Kurnool",
     *   "state": "Andhra Pradesh",
     *   "pincode": "518002",
     *   "aadhaarNumber": "1234 5678 9012",
     *   "bankAccountNumber": "1234567890",
     *   "ifscCode": "SBIN0012345"
     * }
     *
     * Response (201 Created) — same as buyer but role = "SHOP"
     */
    @PostMapping("/register/shop")
    public ResponseEntity<?> registerShop(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.registerShop(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────

    /**
     * Login with email and password.
     *
     * Request Body (JSON):
     * {
     *   "email": "ravi@example.com",
     *   "password": "MyPass123"
     * }
     *
     * Response (200 OK) — JWT token + user info
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────
    // REGISTRATION OTP  (send → verify → then call register/buyer or register/shop)
    // ─────────────────────────────────────

    /**
     * Step 1 of registration: send a 6-digit OTP to the given email or phone.
     * Rejects if that email/phone is already a registered account.
     *
     * Request Body:
     * { "recipient": "ravi@example.com", "channel": "EMAIL", "purpose": "REGISTRATION" }
     * (channel is EMAIL or PHONE; for PHONE, recipient must include country code, e.g. +919876543210)
     */
    @PostMapping("/register/send-otp")
    public ResponseEntity<?> registerSendOtp(@Valid @RequestBody SendOtpRequest request) {
        try {
            authService.initiateRegistrationOtp(request);
            return ResponseEntity.ok(Map.of("message", "OTP sent to " + request.getRecipient()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Step 2 of registration: verify the OTP code before the account is created.
     * Only after this succeeds will /register/buyer or /register/shop accept
     * that email/phone (see AuthService.registerBuyer/registerShop).
     *
     * Request Body:
     * { "recipient": "ravi@example.com", "otpCode": "123456", "purpose": "REGISTRATION" }
     */
    @PostMapping("/register/verify-otp")
    public ResponseEntity<?> registerVerifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            otpService.verifyOtp(request.getRecipient(), request.getOtpCode(), request.getPurpose());
            return ResponseEntity.ok(Map.of("message", "OTP verified — you can now complete registration"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────
    // FORGOT PASSWORD  (send OTP → reset with code + new password)
    // ─────────────────────────────────────

    /**
     * Step 1 of password reset: send a 6-digit OTP to an existing account's
     * email or phone (the user picks which channel). Rejects if no account
     * matches that recipient.
     *
     * Request Body:
     * { "recipient": "ravi@example.com", "channel": "EMAIL", "purpose": "FORGOT_PASSWORD" }
     */
    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<?> forgotPasswordSendOtp(@Valid @RequestBody SendOtpRequest request) {
        try {
            authService.initiatePasswordResetOtp(request);
            return ResponseEntity.ok(Map.of("message", "OTP sent to " + request.getRecipient()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Step 2 of password reset: verify the OTP and set the new password in
     * one call.
     *
     * Request Body:
     * { "recipient": "ravi@example.com", "otpCode": "123456", "newPassword": "NewPass123", "role": "BUYER" }
     */
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request);
            return ResponseEntity.ok(Map.of("message", "Password reset successful — please log in with your new password"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────
    // HEALTH CHECK (for testing)
    // ─────────────────────────────────────

    /**
     * Simple ping to verify backend is running.
     * GET /api/auth/ping → 200 OK with "pong"
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("FarmFresh Backend is running! 🌿");
    }
}
