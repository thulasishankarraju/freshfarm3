package com.example.freshfarm3.enums;

public enum PaymentStatus {
    CREATED,       // Razorpay order created, user hasn't paid yet
    PENDING,       // Payment initiated, awaiting confirmation
    SUCCESS,       // Payment verified and confirmed
    FAILED,        // Payment failed or verification failed
    REFUNDED       // Payment refunded
}
