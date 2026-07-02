package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.response.AuthResponse;
import com.example.freshfarm3.dto.request.LoginRequest;
import com.example.freshfarm3.dto.request.RegisterRequest;
import com.example.freshfarm3.service.AuthService;
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
 *   POST /api/auth/register/farmer   → Register a new farmer
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
    // FARMER REGISTRATION
    // ─────────────────────────────────────

    /**
     * Register a new Farmer.
     *
     * Request Body (JSON):
     * {
     *   "fullName": "Suresh Patel",
     *   "email": "suresh@farm.com",
     *   "phone": "9123456780",
     *   "password": "FarmPass123",
     *   "farmName": "Green Fields Farm",
     *   "village": "Vangoor",
     *   "district": "Kurnool",
     *   "state": "Andhra Pradesh",
     *   "pincode": "518002",
     *   "aadhaarNumber": "1234 5678 9012",
     *   "bankAccountNumber": "1234567890",
     *   "ifscCode": "SBIN0012345"
     * }
     *
     * Response (201 Created) — same as buyer but role = "FARMER"
     */
    @PostMapping("/register/farmer")
    public ResponseEntity<?> registerFarmer(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.registerFarmer(request);
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
