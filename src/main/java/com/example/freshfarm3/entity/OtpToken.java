package com.example.freshfarm3.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The email or phone the OTP was sent to
    @Column(nullable = false)
    private String recipient;          // e.g. "user@gmail.com" or "+919876543210"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpChannel channel;        // EMAIL or PHONE

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpPurpose purpose;        // REGISTRATION or FORGOT_PASSWORD

    @Column(nullable = false, length = 6)
    private String otpCode;            // 6-digit code, stored as plain text (short-lived)

    @Column(nullable = false)
    private LocalDateTime expiresAt;   // created_at + 10 minutes

    @Column(nullable = false)
    private boolean used = false;      // flipped to true once verified

    @Column
    private String userRole;           // "BUYER" or "FARMER" — needed for forgot-password

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    // Channel enum — lives here for simplicity, move to enums/ if needed
    public enum OtpChannel { EMAIL, PHONE }

    // Purpose enum
    public enum OtpPurpose { REGISTRATION, FORGOT_PASSWORD }
}
