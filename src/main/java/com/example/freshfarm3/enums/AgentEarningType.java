package com.example.freshfarm3.enums;

/**
 * AgentEarningType — What a given AgentEarning row represents.
 *
 * DELIVERY_FEE — The amount an agent earns for completing one delivery
 *                (flat fee + any buyer tip).
 * BONUS        — Extra incentive credited automatically once an agent
 *                completes every 5th delivery of the day (see
 *                DeliveryService.AGENT_BONUS_EVERY_N_DELIVERIES).
 */
public enum AgentEarningType {
    DELIVERY_FEE,
    BONUS
}

