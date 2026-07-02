package com.example.freshfarm3.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Size(max = 150, message = "Review title must not exceed 150 characters")
    private String reviewTitle;

    @Size(max = 2000, message = "Review comment must not exceed 2000 characters")
    private String reviewComment;
}
