package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.OtpToken;
import com.example.freshfarm3.entity.OtpToken.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    // Used when generating a new OTP — clears out any stale/unused ones first
    void deleteAllByRecipientAndPurpose(String recipient, OtpPurpose purpose);

    // Used during verification — grabs the most recent unused OTP for this recipient+purpose
    Optional<OtpToken> findTopByRecipientAndPurposeAndUsedFalseOrderByIdDesc(
            String recipient, OtpPurpose purpose);

    // Used to confirm OTP was verified before allowing registration/reset to proceed
    boolean existsByRecipientAndPurposeAndUsedTrue(String recipient, OtpPurpose purpose);
}