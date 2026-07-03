package com.example.freshfarm3.repository;

import com.farmfresh3.entity.OtpToken;
import com.farmfresh3.entity.OtpToken.OtpChannel;
import com.farmfresh3.entity.OtpToken.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    // Find the latest unused OTP for this recipient + purpose
    Optional<OtpToken> findTopByRecipientAndPurposeAndUsedFalseOrderByIdDesc(
            String recipient, OtpPurpose purpose);

    // Delete all old OTPs for a recipient before issuing a new one
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpToken o WHERE o.recipient = :recipient AND o.purpose = :purpose")
    void deleteAllByRecipientAndPurpose(String recipient, OtpPurpose purpose);

    // Check if a verified (used=true) OTP exists — used to confirm registration completed
    boolean existsByRecipientAndPurposeAndUsedTrue(String recipient, OtpPurpose purpose);
}
