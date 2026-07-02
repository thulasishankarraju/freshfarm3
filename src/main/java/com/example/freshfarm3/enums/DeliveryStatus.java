package com.example.freshfarm3.enums;

public enum DeliveryStatus {
    ASSIGNED,          // Agent assigned, hasn't picked up yet
    PICKED_UP,         // Agent has picked up the order from farmer
    OUT_FOR_DELIVERY,  // Agent is en route to buyer
    DELIVERED,         // Delivered — OTP verified
    CANCELLED          // Delivery cancelled or reassigned
}
