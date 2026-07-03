package com.example.freshfarm3.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AuthResponseDto — Response sent to client after login or registration.
 *
 * The frontend stores the token in localStorage and attaches it
 * to every subsequent request as: Authorization: Bearer <token>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    /** JWT Bearer token — client stores this in localStorage */
    private String token;

    /** Token type — always "Bearer" */
    private String tokenType = "Bearer";

    /** Logged-in user's database ID */
    private Long userId;

    /** Logged-in user's full name (display in UI) */
    private String fullName;

    /** Logged-in user's email */
    private String email;

    /** Role: FARMER / BUYER / AGENT / ADMIN (controls which pages user sees) */
    private String role;

    /** Token expiry in milliseconds (24 hours = 86400000) */
    private Long expiresIn;

    public AuthResponse(String token, String name, String fullName, Long id) {
    }
}