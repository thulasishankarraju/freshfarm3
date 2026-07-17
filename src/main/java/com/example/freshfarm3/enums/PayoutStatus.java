package com.example.freshfarm3.enums;

/**
 * PayoutStatus — Status of an admin-to-shop settlement payout.
 *
 * PENDING — Admin has fixed the amount owed to the shop, but has not
 *           yet sent the money.
 * PAID    — Admin has sent the amount to the shop's bank account.
 */
public enum PayoutStatus {
    PENDING,
    PAID
}

