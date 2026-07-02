package com.example.freshfarm3.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeliveryRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    // Optional — if null, system auto-assigns an available agent
    private Long agentId;
}

