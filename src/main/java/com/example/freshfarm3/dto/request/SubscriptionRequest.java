package com.example.freshfarm3.dto.request;

import com.example.freshfarm3.enums.SubscriptionFrequency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Frequency is required")
    private SubscriptionFrequency frequency;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;
}
