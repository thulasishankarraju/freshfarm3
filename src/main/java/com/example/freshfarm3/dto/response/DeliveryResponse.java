package com.example.freshfarm3.dto.response;

import com.example.freshfarm3.enums.DeliveryStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DeliveryResponse {

    private Long           deliveryId;
    private Long           orderId;
    private String         orderNumber;
    private Long           agentId;
    private String         agentName;
    private String         agentPhone;
    private String         vehicleNumber;
    private String         vehicleType;
    private DeliveryStatus deliveryStatus;
    private LocalDateTime  assignedAt;
    private LocalDateTime  pickedUpAt;
    private LocalDateTime  deliveredAt;
    private LocalDateTime  estimatedDeliveryTime;
    private Boolean        otpVerified;

    // OTP is only included in the agent's pickup response — not in public tracking
    private String         otp;
}

