package com.example.freshfarm3.enums;

/**
 * Role — User roles in the FarmFresh platform.
 *
 * Used by:
 *   - User entity (stored in DB as VARCHAR)
 *   - JWT token (stored as a claim)
 *   - SecurityConfig (route access control)
 *   - @PreAuthorize annotations in controllers
 */
public enum Role {

    /**
     * FARMER — Lists products, views orders, tracks earnings.
     * Must be approved by ADMIN before listing products.
     */
    FARMER,

    /**
     * BUYER — Browses products, adds to cart, places orders, pays.
     */
    BUYER,

    /**
     * AGENT — Picks up and delivers orders.
     */
    AGENT,

    /**
     * ADMIN — Full platform control. Approves farmers, manages all data.
     */
    ADMIN
}