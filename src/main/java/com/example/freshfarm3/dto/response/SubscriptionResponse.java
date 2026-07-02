package com.example.freshfarm3.dto.response;

import com.example.freshfarm3.enums.SubscriptionFrequency;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponse {

    private Long id;
    private Long buyerId;
    private String buyerName;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Integer quantity;
    private SubscriptionFrequency frequency;
    private LocalDate startDate;
    private LocalDate nextDeliveryDate;
    private boolean active;
    private boolean paused;
    private LocalDateTime createdDate;
}
