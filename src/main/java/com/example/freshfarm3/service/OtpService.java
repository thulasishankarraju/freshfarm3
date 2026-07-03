package com.example.freshfarm3.service;

import com.freshfarm3.entity.OtpToken;
import com.freshfarm3.entity.OtpToken.OtpChannel;
import com.freshfarm3.entity.OtpToken.OtpPurpose;
import com.freshfarm3.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final JavaMailSender mailSender;
    private final SmsService smsService;   // Twilio wrapper already in your project

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    // ── STEP 1: Generate and send OTP ──────────────────────────────────────
    public void sendOtp(String recipient, String channelStr, String purposeStr, String role) {

        OtpChannel channel = OtpChannel.valueOf(channelStr.toUpperCase());
        OtpPurpose purpose = OtpPurpose.valueOf(purposeStr.toUpperCase());

        // Validate recipient format before doing anything
        if (channel == OtpChannel.EMAIL) {
            if (!recipient.contains("@")) {
                throw new IllegalArgumentException("Invalid email address");
            }
        } else {
            // Phone must start with + and be at least 10 digits
            if (!recipient.startsWith("+") || recipient.length() < 10) {
                throw new IllegalArgumentException(
                        "Phone must include country code e.g. +919876543210");
            }
        }

        // For FORGOT_PASSWORD, verify the account actually exists first
        // (prevents OTP spam on non-existent accounts)
        // This check is done in AuthService before calling sendOtp()

        // Delete any previous OTPs for this recipient + purpose
        otpTokenRepository.deleteAllByRecipientAndPurpose(recipient, purpose);

        // Generate a secure 6-digit code
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        // Save to DB
        OtpToken token = OtpToken.builder()
                .recipient(recipient)
                .channel(channel)
                .purpose(purpose)
                .otpCode(code)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .used(false)
                .userRole(role != null ? role.toUpperCase() : null)
                .build();
        otpTokenRepository.save(token);

        // Send via the right channel
        if (channel == OtpChannel.EMAIL) {
            sendEmail(recipient, code, purpose);
        } else {
            sendSms(recipient, code, purpose);
        }

        log.info("OTP sent to {} via {} for purpose {}", recipient, channel, purpose);
    }

    // ── STEP 2: Verify OTP ─────────────────────────────────────────────────
    // Returns true if valid, throws exception with reason if not
    public void verifyOtp(String recipient, String code, String purposeStr) {

        OtpPurpose purpose = OtpPurpose.valueOf(purposeStr.toUpperCase());

        OtpToken token = otpTokenRepository
                .findTopByRecipientAndPurposeAndUsedFalseOrderByIdDesc(recipient, purpose)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No OTP found. Please request a new one."));

        if (token.isExpired()) {
            throw new IllegalArgumentException(
                    "OTP has expired. Please request a new one.");
        }

        if (!token.getOtpCode().equals(code)) {
            throw new IllegalArgumentException("Incorrect OTP. Please try again.");
        }

        // Mark as used — cannot be reused
        token.setUsed(true);
        otpTokenRepository.save(token);
    }

    // ── Check if OTP was verified (for registration flow guard) ───────────
    public boolean isOtpVerified(String recipient, String purposeStr) {
        OtpPurpose purpose = OtpPurpose.valueOf(purposeStr.toUpperCase());
        return otpTokenRepository
                .existsByRecipientAndPurposeAndUsedTrue(recipient, purpose);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private void sendEmail(String toEmail, String code, OtpPurpose purpose) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("farmfresh.alerts@gmail.com");
        msg.setTo(toEmail);

        if (purpose == OtpPurpose.REGISTRATION) {
            msg.setSubject("FarmFresh — Your Registration OTP");
            msg.setText(
                    "Welcome to FarmFresh! 🌿\n\n" +
                            "Your OTP for registration is:\n\n" +
                            "  " + code + "\n\n" +
                            "This code expires in " + OTP_EXPIRY_MINUTES + " minutes.\n" +
                            "Do not share this code with anyone.\n\n" +
                            "— The FarmFresh Team"
            );
        } else {
            msg.setSubject("FarmFresh — Password Reset OTP");
            msg.setText(
                    "Hi,\n\n" +
                            "We received a request to reset your FarmFresh password.\n\n" +
                            "Your OTP is:\n\n" +
                            "  " + code + "\n\n" +
                            "This code expires in " + OTP_EXPIRY_MINUTES + " minutes.\n" +
                            "If you did not request this, please ignore this email.\n\n" +
                            "— The FarmFresh Team"
            );
        }

        mailSender.send(msg);
    }

    private void sendSms(String toPhone, String code, OtpPurpose purpose) {
        String message;
        if (purpose == OtpPurpose.REGISTRATION) {
            message = "FarmFresh: Your registration OTP is " + code +
                    ". Valid for " + OTP_EXPIRY_MINUTES + " minutes. Do not share.";
        } else {
            message = "FarmFresh: Your password reset OTP is " + code +
                    ". Valid for " + OTP_EXPIRY_MINUTES + " minutes. Do not share.";
        }
        smsService.send(toPhone, message);   // your existing Twilio wrapper
    }
}
