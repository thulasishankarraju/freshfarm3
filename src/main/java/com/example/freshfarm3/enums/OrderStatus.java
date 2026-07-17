package com.example.freshfarm3.enums;

public enum OrderStatus {
    PENDING,            // Order placed, awaiting admin confirmation
    CONFIRMED,          // Admin confirmed the order; shop notified to pack it
    PROCESSING,         // Shop has packed it / delivery is being assigned
    SHIPPED,            // Handed to delivery agent (picked up)
    OUT_FOR_DELIVERY,   // Agent is en route; buyer has been sent the OTP
    DELIVERED,          // Delivered and OTP verified
    CANCELLED           // Cancelled by buyer or system
}