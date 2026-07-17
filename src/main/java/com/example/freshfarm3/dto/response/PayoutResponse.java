package com.example.freshfarm3.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutResponse {
    private Long id;
    private Long shopId;
    private String shopName;
    private BigDecimal amount;
    private String status;       // PENDING / PAID
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
