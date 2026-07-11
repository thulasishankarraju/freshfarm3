package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.OtpToken;
import com.example.freshfarm3.entity.OtpToken.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    // Used when generating a new OTP — clears out any stale/unused ones first
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM OtpToken t WHERE t.recipient = :recipient AND t.purpose = :purpose")
    void deleteAllByRecipientAndPurpose(@Param("recipient") String recipient, @Param("purpose") OtpPurpose purpose);

    // Used during verification — grabs the most recent unused OTP for this recipient+purpose
    Optional<OtpToken> findTopByRecipientAndPurposeAndUsedFalseOrderByIdDesc(
            String recipient, OtpPurpose purpose);

    // Used to confirm OTP was verified before allowing registration/reset to proceed
    boolean existsByRecipientAndPurposeAndUsedTrue(String recipient, OtpPurpose purpose);
}