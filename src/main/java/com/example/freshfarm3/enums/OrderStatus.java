package com.example.freshfarm3.enums;

public enum OrderStatus {
    PENDING,       // Order placed, awaiting farmer confirmation
    CONFIRMED,     // Farmer confirmed the order
    PROCESSING,    // Being packed / prepared
    SHIPPED,       // Handed to delivery agent
    DELIVERED,     // Delivered and OTP verified
    OUT_FOR_DELIVERY, CANCELLED      // Cancelled by buyer or system
}